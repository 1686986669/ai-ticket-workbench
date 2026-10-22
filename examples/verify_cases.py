"""Acceptance checks against a running real Java HTTP service, not MockMvc."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request,urlopen
from uuid import uuid4

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--base-url',default='http://127.0.0.1:8087')
    args=parser.parse_args();base=args.base_url.rstrip('/')
    if not (base.startswith('http://127.0.0.1:') or base.startswith('http://localhost:')):
        parser.error('Use a local demo service; the checks create test tickets')
    run_id=uuid4().hex[:10]
    def api(method,path,body=None):
        request=Request(base+'/api'+path,data=None if body is None else json.dumps(body).encode('utf-8'),
                        headers={'Content-Type':'application/json'},method=method)
        try:
            with urlopen(request,timeout=15) as response:return response.status,json.load(response)
        except HTTPError as exc:return exc.code,json.load(exc)
    def create(title,description):
        code,ticket=api('POST','/tickets',{'title':f'[case-{run_id}] '+title,'description':description})
        assert code==201,(code,ticket)
        return ticket
    results=[]
    def check(case_id,title,fn):
        try:results.append(dict(id=case_id,title=title,passed=True,evidence=fn()))
        except Exception as exc:results.append(dict(id=case_id,title=title,passed=False,error=f'{type(exc).__name__}: {exc}'))

    def lifecycle():
        t=create('商品列表查询缓慢','数据库慢查询，EXPLAIN 显示扫描过多数据行')
        assert t['category']=='DATABASE' and t['status']=='OPEN' and t['version']==0
        code,suggestion=api('POST',f'/tickets/{t["id"]}/suggestion')
        assert code==200 and suggestion['mode']=='demo-rule' and 'EXPLAIN' in suggestion['suggestion']
        codes=[]
        for status,version in [('IN_PROGRESS',0),('RESOLVED',1)]:
            code,changed=api('PATCH',f'/tickets/{t["id"]}/status',dict(status=status,version=version))
            assert code==200 and changed['status']==status and changed['version']==version+1
            codes.append(code)
        return dict(ticket_id=t['id'],states=['OPEN','IN_PROGRESS','RESOLVED'],versions=[0,1,2],http_statuses=codes,suggestion=suggestion)
    check('JAVA-01','商城慢查询工单，从创建到解决的完整闭环',lifecycle)

    def concurrency():
        t=create('两名支持人员同时接单','订单接口超时')
        def update(_):return api('PATCH',f'/tickets/{t["id"]}/status',dict(status='IN_PROGRESS',version=0))
        with ThreadPoolExecutor(max_workers=2) as pool:responses=list(pool.map(update,range(2)))
        statuses=sorted(code for code,_ in responses)
        code,stored=api('GET',f'/tickets/{t["id"]}')
        assert statuses==[200,409] and code==200 and stored['version']==1 and stored['status']=='IN_PROGRESS'
        return dict(ticket_id=t['id'],two_client_statuses=statuses,stored_status=stored['status'],stored_version=stored['version'])
    check('JAVA-02','两个客户端持相同 version 同时接单，仅一方成功',concurrency)

    def illegal_state():
        t=create('发布失败待处理','部署后健康检查失败')
        code,error=api('PATCH',f'/tickets/{t["id"]}/status',dict(status='RESOLVED',version=0))
        _,stored=api('GET',f'/tickets/{t["id"]}')
        assert code==409 and stored['status']=='OPEN' and stored['version']==0
        return dict(ticket_id=t['id'],skipped_transition_http=code,error=error,stored_status=stored['status'])
    check('JAVA-03','跳过处理中直接结单被拒绝，数据库状态保持不变',illegal_state)

    def pagination():
        for i in range(6):create(f'分页样例 {i}','测试环境例行维护记录')
        code1,first=api('GET','/tickets?page=1&size=5')
        code2,second=api('GET','/tickets?page=2&size=5')
        ids1=[t['id'] for t in first['items']];ids2=[t['id'] for t in second['items']]
        assert code1==code2==200 and len(ids1)==5 and len(ids2)>=4
        assert not set(ids1)&set(ids2) and ids1==sorted(ids1,reverse=True) and min(ids1)>max(ids2)
        return dict(page1_ids=ids1,page2_ids=ids2,total=first['total'],stable_order='id descending',overlap=[])
    check('JAVA-04','多条工单分页，返回稳定倒序且两页无重复',pagination)

    def invalid_and_sql():
        blank,_=api('POST','/tickets',{'title':' ','description':'test'})
        oversized,_=api('GET','/tickets?size=101')
        missing,_=api('GET','/tickets/9223372036854775806')
        t=create("'; DROP TABLE tickets; --",'用户输入只作为文本保存')
        code,saved=api('GET',f'/tickets/{t["id"]}')
        assert [blank,oversized,missing]==[400,400,404]
        assert code==200 and 'DROP TABLE tickets' in saved['title']
        list_code,_=api('GET','/tickets')
        assert list_code==200
        return dict(blank_title_http=blank,oversize_page_http=oversized,missing_ticket_http=missing,
                    sql_text_saved=saved['title'],table_still_queryable_http=list_code)
    check('JAVA-05','非法请求被拒绝，SQL 特殊字符仅作为工单文本',invalid_and_sql)

    report=dict(data_origin='synthetic test tickets; actual HTTP server with real H2 and MyBatis',
                mode='demo-rule; no live model request',passed=sum(r['passed'] for r in results),total=len(results),cases=results)
    Path(__file__).with_name('results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
    return 0 if report['passed']==report['total'] else 1

if __name__=='__main__':raise SystemExit(main())
