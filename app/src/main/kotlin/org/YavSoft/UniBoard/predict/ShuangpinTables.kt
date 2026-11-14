/*
 * UniBoard — Unicode 输入法
 * Copyright (C) 2026 UniBoard contributors
 *
 * 本程序是自由软件：你可以依据 GNU 通用公共许可证第 3 版
 * 或（你选择的）任何更新版本所规定的条款，重新分发和/或修改它。
 *
 * 本程序基于"希望它有用"的目的分发，但不提供任何担保，
 * 也不承诺适用于特定用途。完整条款见项目根目录的 LICENSE 文件。
 */

package org.YavSoft.UniBoard.predict

/**
 * 双拼码表（由 `tools/gen_shuangpin.py` 生成，请勿手工编辑）。
 *
 * 码表来自 Rime 各方案的拼写代数（rime/rime-double-pinyin，GPL-3.0）：
 * 把「全拼音节」按 erase / derive / xform / xlit 规则跑一遍，
 * 得到「两键码 → 音节」。零声母、ü/v、iu/ui、uang/iang 等边界都由规则保证。
 */
internal object ShuangpinTables {

    /** `键位码=音节`，空白分隔；同一码对应多个音节时用 `|` 连接 */
    private const val XIAOHE =
        "aa=a ac=ao ad=ai ah=ang ai=ai aj=an an=an ao=ao ba=ba bb=bin bc=bao bd=bai bf=ben bg=beng " +
        "bh=bang bi=bi bj=ban bk=bing bm=bian bn=biao bo=bo bp=bie bu=bu bw=bei ca=ca cc=cao cd=cai ce=ce " +
        "cf=cen cg=ceng ch=cang ci=ci cj=can co=cuo cr=cuan cs=cong cu=cu cv=cui cy=cun cz=cou da=da " +
        "dc=dao dd=dai de=de df=den dg=deng dh=dang di=di dj=dan dk=ding dm=dian dn=diao do=duo dp=die " +
        "dq=diu dr=duan ds=dong du=du dv=dui dw=dei dy=dun dz=dou ee=e ef=en eg=eng ei=ei en=en er=er " +
        "ew=ei fa=fa ff=fen fg=feng fh=fang fj=fan fo=fo fu=fu fw=fei fz=fou ga=ga gc=gao gd=gai ge=ge " +
        "gf=gen gg=geng gh=gang gj=gan gk=guai gl=guang go=guo gr=guan gs=gong gu=gu gv=gui gw=gei gx=gua " +
        "gy=gun gz=gou ha=ha hc=hao hd=hai he=he hf=hen hg=heng hh=hang hj=han hk=huai hl=huang ho=huo " +
        "hr=huan hs=hong hu=hu hv=hui hw=hei hx=hua hy=hun hz=hou ia=cha ic=chao id=chai ie=che if=chen " +
        "ig=cheng ih=chang ii=chi ij=chan ik=chuai il=chuang io=chuo ir=chuan is=chong iu=chu iv=chui " +
        "ix=chua iy=chun iz=chou jb=jin ji=ji jk=jing jl=jiang jm=jian jn=jiao jp=jie jq=jiu jr=juan " +
        "js=jiong jt=jue ju=ju jv=ju jx=jia jy=jun ka=ka kc=kao kd=kai ke=ke kf=ken kg=keng kh=kang " +
        "kj=kan kk=kuai kl=kuang ko=kuo kr=kuan ks=kong ku=ku kv=kui kw=kei kx=kua ky=kun kz=kou la=la " +
        "lb=lin lc=lao ld=lai le=le lg=leng lh=lang li=li lj=lan lk=ling ll=liang lm=lian ln=liao lo=luo " +
        "lp=lie lq=liu lr=luan ls=long lt=lve lu=lu lv=lv lw=lei lx=lia ly=lun lz=lou ma=ma mb=min mc=mao " +
        "md=mai me=me mf=men mg=meng mh=mang mi=mi mj=man mk=ming mm=mian mn=miao mo=mo mp=mie mq=miu " +
        "mu=mu mw=mei mz=mou na=na nb=nin nc=nao nd=nai ne=ne nf=nen ng=neng nh=nang ni=ni nj=nan nk=ning " +
        "nl=niang nm=nian nn=niao no=nuo np=nie nq=niu nr=nuan ns=nong nt=nve nu=nu nv=nv nw=nei ny=nun " +
        "nz=nou oo=o ou=ou oz=ou pa=pa pb=pin pc=pao pd=pai pf=pen pg=peng ph=pang pi=pi pj=pan pk=ping " +
        "pm=pian pn=piao po=po pp=pie pu=pu pw=pei pz=pou qb=qin qi=qi qk=qing ql=qiang qm=qian qn=qiao " +
        "qp=qie qq=qiu qr=quan qs=qiong qt=que qu=qu qv=qu qx=qia qy=qun rc=rao re=re rf=ren rg=reng " +
        "rh=rang ri=ri rj=ran ro=ruo rr=ruan rs=rong ru=ru rv=rui rx=rua ry=run rz=rou sa=sa sc=sao " +
        "sd=sai se=se sf=sen sg=seng sh=sang si=si sj=san so=suo sr=suan ss=song su=su sv=sui sy=sun " +
        "sz=sou ta=ta tc=tao td=tai te=te tg=teng th=tang ti=ti tj=tan tk=ting tm=tian tn=tiao to=tuo " +
        "tp=tie tr=tuan ts=tong tu=tu tv=tui tw=tei ty=tun tz=tou ua=sha uc=shao ud=shai ue=she uf=shen " +
        "ug=sheng uh=shang ui=shi uj=shan uk=shuai ul=shuang uo=shuo ur=shuan uu=shu uv=shui uw=shei " +
        "ux=shua uy=shun uz=shou va=zha vc=zhao vd=zhai ve=zhe vf=zhen vg=zheng vh=zhang vi=zhi vj=zhan " +
        "vk=zhuai vl=zhuang vo=zhuo vr=zhuan vs=zhong vu=zhu vv=zhui vw=zhei vx=zhua vy=zhun vz=zhou " +
        "wa=wa wd=wai wf=wen wg=weng wh=wang wj=wan wo=wo wu=wu ww=wei xb=xin xi=xi xk=xing xl=xiang " +
        "xm=xian xn=xiao xp=xie xq=xiu xr=xuan xs=xiong xt=xue xu=xu xv=xu xx=xia xy=xun ya=ya yb=yin " +
        "yc=yao ye=ye yh=yang yi=yi yj=yan yk=ying yr=yuan ys=yong yt=yue yu=yu yv=yu yy=yun yz=you za=za " +
        "zc=zao zd=zai ze=ze zf=zen zg=zeng zh=zang zi=zi zj=zan zo=zuo zr=zuan zs=zong zu=zu zv=zui " +
        "zw=zei zy=zun zz=zou"
    private const val ZIRANMA =
        "aa=a ah=ang ai=ai aj=an ak=ao al=ai an=an ao=ao ba=ba bc=biao bf=ben bg=beng bh=bang bi=bi " +
        "bj=ban bk=bao bl=bai bm=bian bn=bin bo=bo bu=bu bx=bie by=bing bz=bei ca=ca cb=cou ce=ce cf=cen " +
        "cg=ceng ch=cang ci=ci cj=can ck=cao cl=cai co=cuo cp=cun cr=cuan cs=cong cu=cu cv=cui da=da " +
        "db=dou dc=diao de=de df=den dg=deng dh=dang di=di dj=dan dk=dao dl=dai dm=dian do=duo dp=dun " +
        "dq=diu dr=duan ds=dong du=du dv=dui dx=die dy=ding dz=dei ee=e ef=en eg=eng ei=ei en=en er=er " +
        "ez=ei fa=fa fb=fou ff=fen fg=feng fh=fang fj=fan fo=fo fu=fu fz=fei ga=ga gb=gou gd=guang ge=ge " +
        "gf=gen gg=geng gh=gang gj=gan gk=gao gl=gai go=guo gp=gun gr=guan gs=gong gu=gu gv=gui gw=gua " +
        "gy=guai gz=gei ha=ha hb=hou hd=huang he=he hf=hen hg=heng hh=hang hj=han hk=hao hl=hai ho=huo " +
        "hp=hun hr=huan hs=hong hu=hu hv=hui hw=hua hy=huai hz=hei ia=cha ib=chou id=chuang ie=che " +
        "if=chen ig=cheng ih=chang ii=chi ij=chan ik=chao il=chai io=chuo ip=chun ir=chuan is=chong " +
        "iu=chu iv=chui iw=chua iy=chuai jc=jiao jd=jiang ji=ji jm=jian jn=jin jp=jun jq=jiu jr=juan " +
        "js=jiong jt=jue ju=ju jv=ju jw=jia jx=jie jy=jing ka=ka kb=kou kd=kuang ke=ke kf=ken kg=keng " +
        "kh=kang kj=kan kk=kao kl=kai ko=kuo kp=kun kr=kuan ks=kong ku=ku kv=kui kw=kua ky=kuai kz=kei " +
        "la=la lb=lou lc=liao ld=liang le=le lg=leng lh=lang li=li lj=lan lk=lao ll=lai lm=lian ln=lin " +
        "lo=luo lp=lun lq=liu lr=luan ls=long lt=lve lu=lu lv=lv lw=lia lx=lie ly=ling lz=lei ma=ma " +
        "mb=mou mc=miao me=me mf=men mg=meng mh=mang mi=mi mj=man mk=mao ml=mai mm=mian mn=min mo=mo " +
        "mq=miu mu=mu mx=mie my=ming mz=mei na=na nb=nou nc=niao nd=niang ne=ne nf=nen ng=neng nh=nang " +
        "ni=ni nj=nan nk=nao nl=nai nm=nian nn=nin no=nuo np=nun nq=niu nr=nuan ns=nong nt=nve nu=nu " +
        "nv=nv nx=nie ny=ning nz=nei ob=ou oo=o ou=ou pa=pa pb=pou pc=piao pf=pen pg=peng ph=pang pi=pi " +
        "pj=pan pk=pao pl=pai pm=pian pn=pin po=po pu=pu px=pie py=ping pz=pei qc=qiao qd=qiang qi=qi " +
        "qm=qian qn=qin qp=qun qq=qiu qr=quan qs=qiong qt=que qu=qu qv=qu qw=qia qx=qie qy=qing rb=rou " +
        "re=re rf=ren rg=reng rh=rang ri=ri rj=ran rk=rao ro=ruo rp=run rr=ruan rs=rong ru=ru rv=rui " +
        "rw=rua sa=sa sb=sou se=se sf=sen sg=seng sh=sang si=si sj=san sk=sao sl=sai so=suo sp=sun " +
        "sr=suan ss=song su=su sv=sui ta=ta tb=tou tc=tiao te=te tg=teng th=tang ti=ti tj=tan tk=tao " +
        "tl=tai tm=tian to=tuo tp=tun tr=tuan ts=tong tu=tu tv=tui tx=tie ty=ting tz=tei ua=sha ub=shou " +
        "ud=shuang ue=she uf=shen ug=sheng uh=shang ui=shi uj=shan uk=shao ul=shai uo=shuo up=shun " +
        "ur=shuan uu=shu uv=shui uw=shua uy=shuai uz=shei va=zha vb=zhou vd=zhuang ve=zhe vf=zhen " +
        "vg=zheng vh=zhang vi=zhi vj=zhan vk=zhao vl=zhai vo=zhuo vp=zhun vr=zhuan vs=zhong vu=zhu " +
        "vv=zhui vw=zhua vy=zhuai vz=zhei wa=wa wf=wen wg=weng wh=wang wj=wan wl=wai wo=wo wu=wu wz=wei " +
        "xc=xiao xd=xiang xi=xi xm=xian xn=xin xp=xun xq=xiu xr=xuan xs=xiong xt=xue xu=xu xv=xu xw=xia " +
        "xx=xie xy=xing ya=ya yb=you ye=ye yh=yang yi=yi yj=yan yk=yao yn=yin yp=yun yr=yuan ys=yong " +
        "yt=yue yu=yu yv=yu yy=ying za=za zb=zou ze=ze zf=zen zg=zeng zh=zang zi=zi zj=zan zk=zao zl=zai " +
        "zo=zuo zp=zun zr=zuan zs=zong zu=zu zv=zui zz=zei"
    private const val MSPY =
        "aa=a ah=ang aj=an ak=ao al=ai b;=bing ba=ba bc=biao bf=ben bg=beng bh=bang bi=bi bj=ban bk=bao " +
        "bl=bai bm=bian bn=bin bo=bo bu=bu bx=bie bz=bei ca=ca cb=cou ce=ce cf=cen cg=ceng ch=cang ci=ci " +
        "cj=can ck=cao cl=cai co=cuo cp=cun cr=cuan cs=cong cu=cu cv=cui d;=ding da=da db=dou dc=diao " +
        "de=de df=den dg=deng dh=dang di=di dj=dan dk=dao dl=dai dm=dian do=duo dp=dun dq=diu dr=duan " +
        "ds=dong du=du dv=dui dx=die dz=dei ee=e ef=en eg=eng er=er ez=ei fa=fa fb=fou ff=fen fg=feng " +
        "fh=fang fj=fan fo=fo fu=fu fz=fei ga=ga gb=gou gd=guang ge=ge gf=gen gg=geng gh=gang gj=gan " +
        "gk=gao gl=gai go=guo gp=gun gr=guan gs=gong gu=gu gv=gui gw=gua gy=guai gz=gei ha=ha hb=hou " +
        "hd=huang he=he hf=hen hg=heng hh=hang hj=han hk=hao hl=hai ho=huo hp=hun hr=huan hs=hong hu=hu " +
        "hv=hui hw=hua hy=huai hz=hei ia=cha ib=chou id=chuang ie=che if=chen ig=cheng ih=chang ii=chi " +
        "ij=chan ik=chao il=chai io=chuo ip=chun ir=chuan is=chong iu=chu iv=chui iw=chua iy=chuai " +
        "j;=jing jc=jiao jd=jiang ji=ji jm=jian jn=jin jp=jun jq=jiu jr=juan js=jiong jt=jue ju=ju jv=jue " +
        "jw=jia jx=jie jy=ju ka=ka kb=kou kd=kuang ke=ke kf=ken kg=keng kh=kang kj=kan kk=kao kl=kai " +
        "ko=kuo kp=kun kr=kuan ks=kong ku=ku kv=kui kw=kua ky=kuai kz=kei l;=ling la=la lb=lou lc=liao " +
        "ld=liang le=le lg=leng lh=lang li=li lj=lan lk=lao ll=lai lm=lian ln=lin lo=luo lp=lun lq=liu " +
        "lr=luan ls=long lt=lve lu=lu lv=lve lw=lia lx=lie ly=lv lz=lei m;=ming ma=ma mb=mou mc=miao " +
        "me=me mf=men mg=meng mh=mang mi=mi mj=man mk=mao ml=mai mm=mian mn=min mo=mo mq=miu mu=mu mx=mie " +
        "mz=mei n;=ning na=na nb=nou nc=niao nd=niang ne=ne nf=nen ng=neng nh=nang ni=ni nj=nan nk=nao " +
        "nl=nai nm=nian nn=nin no=nuo np=nun nq=niu nr=nuan ns=nong nt=nve nu=nu nv=nve nx=nie ny=nv " +
        "nz=nei oa=a ob=ou oe=e of=en og=eng oh=ang oj=an ok=ao ol=ai oo=o or=er ou=ou oz=ei p;=ping " +
        "pa=pa pb=pou pc=piao pf=pen pg=peng ph=pang pi=pi pj=pan pk=pao pl=pai pm=pian pn=pin po=po " +
        "pu=pu px=pie pz=pei q;=qing qc=qiao qd=qiang qi=qi qm=qian qn=qin qp=qun qq=qiu qr=quan qs=qiong " +
        "qt=que qu=qu qv=que qw=qia qx=qie qy=qu rb=rou re=re rf=ren rg=reng rh=rang ri=ri rj=ran rk=rao " +
        "ro=ruo rp=run rr=ruan rs=rong ru=ru rv=rui rw=rua sa=sa sb=sou se=se sf=sen sg=seng sh=sang " +
        "si=si sj=san sk=sao sl=sai so=suo sp=sun sr=suan ss=song su=su sv=sui t;=ting ta=ta tb=tou " +
        "tc=tiao te=te tg=teng th=tang ti=ti tj=tan tk=tao tl=tai tm=tian to=tuo tp=tun tr=tuan ts=tong " +
        "tu=tu tv=tui tx=tie tz=tei ua=sha ub=shou ud=shuang ue=she uf=shen ug=sheng uh=shang ui=shi " +
        "uj=shan uk=shao ul=shai uo=shuo up=shun ur=shuan uu=shu uv=shui uw=shua uy=shuai uz=shei va=zha " +
        "vb=zhou vd=zhuang ve=zhe vf=zhen vg=zheng vh=zhang vi=zhi vj=zhan vk=zhao vl=zhai vo=zhuo " +
        "vp=zhun vr=zhuan vs=zhong vu=zhu vv=zhui vw=zhua vy=zhuai vz=zhei wa=wa wf=wen wg=weng wh=wang " +
        "wj=wan wl=wai wo=wo wu=wu wz=wei x;=xing xc=xiao xd=xiang xi=xi xm=xian xn=xin xp=xun xq=xiu " +
        "xr=xuan xs=xiong xt=xue xu=xu xv=xue xw=xia xx=xie xy=xu y;=ying ya=ya yb=you ye=ye yh=yang " +
        "yi=yi yj=yan yk=yao yn=yin yp=yun yr=yuan ys=yong yt=yue yu=yu yv=yue yy=yu za=za zb=zou ze=ze " +
        "zf=zen zg=zeng zh=zang zi=zi zj=zan zk=zao zl=zai zo=zuo zp=zun zr=zuan zs=zong zu=zu zv=zui " +
        "zz=zei"
    private const val ABC =
        "aa=zha ab=zhou ac=zhuai ad=zhua ae=zhe af=zhen ag=zheng ah=zhang ai=zhi aj=zhan ak=zhao al=zhai " +
        "am=zhui an=zhun ao=zhuo ap=zhuan aq=zhei as=zhong at=zhuang au=zhu ba=ba bc=bin bf=ben bg=beng " +
        "bh=bang bi=bi bj=ban bk=bao bl=bai bo=bo bq=bei bu=bu bw=bian bx=bie by=bing bz=biao ca=ca " +
        "cb=cou ce=ce cf=cen cg=ceng ch=cang ci=ci cj=can ck=cao cl=cai cm=cui cn=cun co=cuo cp=cuan " +
        "cs=cong cu=cu da=da db=dou de=de df=den dg=deng dh=dang di=di dj=dan dk=dao dl=dai dm=dui dn=dun " +
        "do=duo dp=duan dq=dei dr=diu ds=dong du=du dw=dian dx=die dy=ding dz=diao ea=cha eb=chou " +
        "ec=chuai ed=chua ee=che ef=chen eg=cheng eh=chang ei=chi ej=chan ek=chao el=chai em=chui en=chun " +
        "eo=chuo ep=chuan es=chong et=chuang eu=chu fa=fa fb=fou ff=fen fg=feng fh=fang fj=fan fo=fo " +
        "fq=fei fu=fu ga=ga gb=gou gc=guai gd=gua ge=ge gf=gen gg=geng gh=gang gj=gan gk=gao gl=gai " +
        "gm=gui gn=gun go=guo gp=guan gq=gei gs=gong gt=guang gu=gu ha=ha hb=hou hc=huai hd=hua he=he " +
        "hf=hen hg=heng hh=hang hj=han hk=hao hl=hai hm=hui hn=hun ho=huo hp=huan hq=hei hs=hong ht=huang " +
        "hu=hu jc=jin jd=jia ji=ji jm=jue jn=jun jp=juan jr=jiu js=jiong jt=jiang ju=ju jv=ju jw=jian " +
        "jx=jie jy=jing jz=jiao ka=ka kb=kou kc=kuai kd=kua ke=ke kf=ken kg=keng kh=kang kj=kan kk=kao " +
        "kl=kai km=kui kn=kun ko=kuo kp=kuan kq=kei ks=kong kt=kuang ku=ku la=la lb=lou lc=lin ld=lia " +
        "le=le lg=leng lh=lang li=li lj=lan lk=lao ll=lai lm=lve ln=lun lo=luo lp=luan lq=lei lr=liu " +
        "ls=long lt=liang lu=lu lv=lv lw=lian lx=lie ly=ling lz=liao ma=ma mb=mou mc=min me=me mf=men " +
        "mg=meng mh=mang mi=mi mj=man mk=mao ml=mai mo=mo mq=mei mr=miu mu=mu mw=mian mx=mie my=ming " +
        "mz=miao na=na nb=nou nc=nin ne=ne nf=nen ng=neng nh=nang ni=ni nj=nan nk=nao nl=nai nm=nve " +
        "nn=nun no=nuo np=nuan nq=nei nr=niu ns=nong nt=niang nu=nu nv=nv nw=nian nx=nie ny=ning nz=niao " +
        "oa=a ob=ou oe=e of=en og=eng oh=ang oj=an ok=ao ol=ai oo=o oq=ei or=er pa=pa pb=pou pc=pin " +
        "pf=pen pg=peng ph=pang pi=pi pj=pan pk=pao pl=pai po=po pq=pei pu=pu pw=pian px=pie py=ping " +
        "pz=piao qc=qin qd=qia qi=qi qm=que qn=qun qp=quan qr=qiu qs=qiong qt=qiang qu=qu qv=qu qw=qian " +
        "qx=qie qy=qing qz=qiao rb=rou rd=rua re=re rf=ren rg=reng rh=rang ri=ri rj=ran rk=rao rm=rui " +
        "rn=run ro=ruo rp=ruan rs=rong ru=ru sa=sa sb=sou se=se sf=sen sg=seng sh=sang si=si sj=san " +
        "sk=sao sl=sai sm=sui sn=sun so=suo sp=suan ss=song su=su ta=ta tb=tou te=te tg=teng th=tang " +
        "ti=ti tj=tan tk=tao tl=tai tm=tui tn=tun to=tuo tp=tuan tq=tei ts=tong tu=tu tw=tian tx=tie " +
        "ty=ting tz=tiao va=sha vb=shou vc=shuai vd=shua ve=she vf=shen vg=sheng vh=shang vi=shi vj=shan " +
        "vk=shao vl=shai vm=shui vn=shun vo=shuo vp=shuan vq=shei vt=shuang vu=shu wa=wa wf=wen wg=weng " +
        "wh=wang wj=wan wl=wai wo=wo wq=wei wu=wu xc=xin xd=xia xi=xi xm=xue xn=xun xp=xuan xr=xiu " +
        "xs=xiong xt=xiang xu=xu xv=xu xw=xian xx=xie xy=xing xz=xiao ya=ya yb=you yc=yin ye=ye yh=yang " +
        "yi=yi yj=yan yk=yao ym=yue yn=yun yp=yuan ys=yong yu=yu yv=yu yy=ying za=za zb=zou ze=ze zf=zen " +
        "zg=zeng zh=zang zi=zi zj=zan zk=zao zl=zai zm=zui zn=zun zo=zuo zp=zuan zq=zei zs=zong zu=zu"
    private const val PYJJ =
        "aa=a ad=ao af=an ag=ang as=ai ba=ba bd=bao bf=ban bg=bang bi=bi bj=bian bk=biao bl=bin bm=bie " +
        "bo=bo bq=bing br=ben bs=bai bt=beng bu=bu bw=bei ca=ca cc=cuan cd=cao ce=ce cf=can cg=cang ci=ci " +
        "co=cuo cp=cou cr=cen cs=cai ct=ceng cu=cu cv=cui cy=cong cz=cun da=da dc=duan dd=dao de=de " +
        "df=dan dg=dang di=di dj=dian dk=diao dm=die dn=diu do=duo dp=dou dq=ding dr=den ds=dai dt=deng " +
        "du=du dv=dui dw=dei dy=dong dz=dun ee=e eq=er er=en et=eng ew=ei fa=fa ff=fan fg=fang fo=fo " +
        "fp=fou fr=fen ft=feng fu=fu fw=fei ga=ga gb=gua gc=guan gd=gao ge=ge gf=gan gg=gang gh=guang " +
        "go=guo gp=gou gr=gen gs=gai gt=geng gu=gu gv=gui gw=gei gx=guai gy=gong gz=gun ha=ha hb=hua " +
        "hc=huan hd=hao he=he hf=han hg=hang hh=huang ho=huo hp=hou hr=hen hs=hai ht=heng hu=hu hv=hui " +
        "hw=hei hx=huai hy=hong hz=hun ia=sha ib=shua ic=shuan id=shao ie=she if=shan ig=shang ih=shuang " +
        "ii=shi io=shuo ip=shou ir=shen is=shai it=sheng iu=shu iv=shui iw=shei ix=shuai iz=shun jb=jia " +
        "jc=juan jh=jiang ji=ji jj=jian jk=jiao jl=jin jm=jie jn=jiu jq=jing ju=ju jv=ju jx=jue jy=jiong " +
        "jz=jun ka=ka kb=kua kc=kuan kd=kao ke=ke kf=kan kg=kang kh=kuang ko=kuo kp=kou kr=ken ks=kai " +
        "kt=keng ku=ku kv=kui kw=kei kx=kuai ky=kong kz=kun la=la lb=lia lc=luan ld=lao le=le lf=lan " +
        "lg=lang lh=liang li=li lj=lian lk=liao ll=lin lm=lie ln=liu lo=luo lp=lou lq=ling ls=lai lt=leng " +
        "lu=lu lv=lv lw=lei lx=lve ly=long lz=lun ma=ma md=mao me=me mf=man mg=mang mi=mi mj=mian mk=miao " +
        "ml=min mm=mie mn=miu mo=mo mp=mou mq=ming mr=men ms=mai mt=meng mu=mu mw=mei na=na nc=nuan " +
        "nd=nao ne=ne nf=nan ng=nang nh=niang ni=ni nj=nian nk=niao nl=nin nm=nie nn=niu no=nuo np=nou " +
        "nq=ning nr=nen ns=nai nt=neng nu=nu nv=nv nw=nei nx=nve ny=nong nz=nun oa=a od=ao oe=e of=an " +
        "og=ang oo=o op=ou oq=er or=en os=ai ot=eng ou=ou ow=ei pa=pa pd=pao pf=pan pg=pang pi=pi pj=pian " +
        "pk=piao pl=pin pm=pie po=po pp=pou pq=ping pr=pen ps=pai pt=peng pu=pu pw=pei qb=qia qc=quan " +
        "qh=qiang qi=qi qj=qian qk=qiao ql=qin qm=qie qn=qiu qq=qing qu=qu qv=qu qx=que qy=qiong qz=qun " +
        "rb=rua rc=ruan rd=rao re=re rf=ran rg=rang ri=ri ro=ruo rp=rou rr=ren rt=reng ru=ru rv=rui " +
        "ry=rong rz=run sa=sa sc=suan sd=sao se=se sf=san sg=sang si=si so=suo sp=sou sr=sen ss=sai " +
        "st=seng su=su sv=sui sy=song sz=sun ta=ta tc=tuan td=tao te=te tf=tan tg=tang ti=ti tj=tian " +
        "tk=tiao tm=tie to=tuo tp=tou tq=ting ts=tai tt=teng tu=tu tv=tui tw=tei ty=tong tz=tun ua=cha " +
        "ub=chua uc=chuan ud=chao ue=che uf=chan ug=chang uh=chuang ui=chi uo=chuo up=chou ur=chen " +
        "us=chai ut=cheng uu=chu uv=chui ux=chuai uy=chong uz=chun va=zha vb=zhua vc=zhuan vd=zhao ve=zhe " +
        "vf=zhan vg=zhang vh=zhuang vi=zhi vo=zhuo vp=zhou vr=zhen vs=zhai vt=zheng vu=zhu vv=zhui " +
        "vw=zhei vx=zhuai vy=zhong vz=zhun wa=wa wf=wan wg=wang wo=wo wr=wen ws=wai wt=weng wu=wu ww=wei " +
        "xb=xia xc=xuan xh=xiang xi=xi xj=xian xk=xiao xl=xin xm=xie xn=xiu xq=xing xu=xu xv=xu xx=xue " +
        "xy=xiong xz=xun ya=ya yc=yuan yd=yao ye=ye yf=yan yg=yang yi=yi yl=yin yp=you yq=ying yu=yu " +
        "yv=yu yx=yue yy=yong yz=yun za=za zc=zuan zd=zao ze=ze zf=zan zg=zang zi=zi zo=zuo zp=zou zr=zen " +
        "zs=zai zt=zeng zu=zu zv=zui zw=zei zy=zong zz=zun"

    /** `键位=韵母`，给键盘角标用 */
    private const val XIAOHE_HINTS =
        "a=a b=in c=ao d=ai e=e f=en g=eng h=ang i=i j=an k=ing/uai l=uang/iang m=ian n=iao o=uo/o p=ie " +
        "q=iu r=uan s=ong/iong t=ue/ve u=u v=ui/u w=ei x=ua/ia y=un z=ou"
    private const val ZIRANMA_HINTS =
        "a=a b=ou c=iao d=uang/iang e=e f=en g=eng h=ang i=i j=an k=ao l=ai m=ian n=in o=uo/o p=un q=iu " +
        "r=uan s=ong/iong t=ue/ve u=u v=ui/u w=ua/ia x=ie y=ing/uai z=ei"
    private const val MSPY_HINTS =
        ";=ing a=a b=ou c=iao d=uang/iang e=e f=en g=eng h=ang i=i j=an k=ao l=ai m=ian n=in o=uo/o p=un " +
        "q=iu r=uan s=ong/iong t=ue/ve u=u v=ui/ue w=ua/ia x=ie y=uai/u z=ei"
    private const val ABC_HINTS =
        "a=a b=ou c=in/uai d=ua/ia e=e f=en g=eng h=ang i=i j=an k=ao l=ai m=ui/ue n=un o=uo/o p=uan q=ei " +
        "r=iu s=ong/iong t=uang/iang u=u v=u/v w=ian x=ie y=ing z=iao"
    private const val PYJJ_HINTS =
        "a=a b=ua/ia c=uan d=ao e=e f=an g=ang h=uang/iang i=i j=ian k=iao l=in m=ie n=iu o=uo/o p=ou " +
        "q=ing r=en s=ai t=eng u=u v=ui/u w=ei x=uai/ue y=ong/iong z=un"

    /** 单键零声母（只有 a / o / e 这类音节才会出现；没有的方案留空串） */
    private const val XIAOHE_SINGLE = ""
    private const val ZIRANMA_SINGLE = ""
    private const val MSPY_SINGLE = "o=o"
    private const val ABC_SINGLE = ""
    private const val PYJJ_SINGLE = "o=o"

    /** 一个方案的原始码表 */
    class Raw(val codes: String, val hints: String, val singles: String)

    /** 方案索引 → 原始码表，顺序与 [ShuangpinScheme] 的 id 一致 */
    val RAW: List<Raw> = listOf(
        Raw(XIAOHE, XIAOHE_HINTS, XIAOHE_SINGLE), // 小鹤双拼
        Raw(ZIRANMA, ZIRANMA_HINTS, ZIRANMA_SINGLE), // 自然码
        Raw(MSPY, MSPY_HINTS, MSPY_SINGLE), // 微软双拼
        Raw(ABC, ABC_HINTS, ABC_SINGLE), // 智能 ABC
        Raw(PYJJ, PYJJ_HINTS, PYJJ_SINGLE), // 拼音加加
    )
}
