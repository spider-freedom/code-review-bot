import re

with open('e:/code-review-bot/prototype/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Find the script tag
script_start = content.index('<script>')
script_end = content.index('</script>') + len('</script>')

# New JS section
new_js = '''<script>
// ============================================================
// STATE
// ============================================================
var state = {
  currentRole: null, currentPage: null,
  bannerTimer: null, bannerIdx: 0, echarts: {}
};

var sidebarMenus = {
  student: [
    { icon:'👤', label:'个人主页', page:'profile' },
    { icon:'📋', label:'活动列表', page:'student-activity-list' },
    { icon:'📝', label:'已报名活动', page:'student-my-enrollments' },
    { icon:'💬', label:'我的反馈', page:'student-feedback-detail' }
  ],
  teacher: [
    { icon:'👤', label:'个人主页', page:'profile' },
    { icon:'📋', label:'活动列表', page:'teacher-activity-list' },
    { icon:'📝', label:'已报名活动', page:'teacher-my-enrollments' }
  ],
  college: [
    { icon:'👤', label:'个人主页', page:'profile' },
    { icon:'📋', label:'活动列表', page:'college-activity-list' },
    { icon:'✅', label:'报名审批', page:'college-approval-list' },
    { icon:'💬', label:'反馈管理', page:'college-feedback-list' }
  ],
  school: [
    { icon:'👤', label:'个人主页', page:'profile' },
    { icon:'📊', label:'数据大屏', page:'school-dashboard' },
    { icon:'📋', label:'活动管理', page:'school-activity-list' },
    { icon:'➕', label:'创建活动', page:'school-create-activity' },
    { icon:'✅', label:'报名审批', page:'school-approval-list' },
    { icon:'💬', label:'反馈管理', page:'school-feedback-list' }
  ]
};

var userDisplayNames = {
  student: '艾克拜尔·买买提 (2021001)',
  teacher: '李老师 (T2021001)',
  college: '赵老师 (C2021001)',
  school: '陈老师 (S2021001)'
};
var roleLabels = { student:'学生', teacher:'教师', college:'学院管理员', school:'学校管理员' };

var profileData = {
  student: { avatar:'🎓', name:'艾克拜尔·买买提', fields:[['学号','2021001'],['学院','计算机科学与技术学院'],['专业','软件工程'],['年级','大三'],['绩点','3.7 / 4.0'],['邮箱','aikebai@xju.edu.cn'],['手机','13800001111']] },
  teacher: { avatar:'👨‍🏫', name:'李老师', fields:[['工号','T2021001'],['职称','副教授'],['学院','计算机科学与技术学院'],['邮箱','lilaoshi@xju.edu.cn'],['手机','13900002222']] },
  college: { avatar:'🏛️', name:'赵老师', fields:[['工号','C2021001'],['职务','学院管理员'],['管辖学院','计算机科学与技术学院'],['邮箱','zhaolaoshi@xju.edu.cn']] },
  school: { avatar:'🏫', name:'陈老师', fields:[['工号','S2021001'],['职务','招生办主任'],['邮箱','chenlaoshi@xju.edu.cn'],['管理范围','全校']] }
};

// ============================================================
// LOGIN FLOW
// ============================================================
function showLoginForm(role) {
  document.getElementById('role-selector').style.display = 'none';
  document.querySelectorAll('.login-card-wrap').forEach(function(w){ w.classList.remove('show'); });
  var f = document.getElementById('login-form-'+role);
  if(f) f.classList.add('show');
}
function backToRoleSelect() {
  document.getElementById('role-selector').style.display = '';
  document.querySelectorAll('.login-card-wrap').forEach(function(w){ w.classList.remove('show'); });
}
function showRegister() {
  document.getElementById('role-selector').style.display = 'none';
  document.querySelectorAll('.login-card-wrap').forEach(function(w){ w.classList.remove('show'); });
  document.getElementById('register-form').classList.add('show');
}
function doLogin(role) {
  state.currentRole = role;
  document.getElementById('login-overlay').classList.remove('show');
  renderSidebar(role);
  document.getElementById('display-username').textContent = userDisplayNames[role]||'';
  document.getElementById('display-role').textContent = roleLabels[role]||'';
  document.getElementById('notify-count').style.display = 'inline-flex';
  document.getElementById('notify-count').textContent = '3';
  var pages = { student:'student-activity-list', teacher:'teacher-activity-list', college:'college-activity-list', school:'school-dashboard' };
  navigateTo(pages[role]);
}
function logout() {
  state.currentRole=null; state.currentPage=null;
  document.querySelectorAll('.page').forEach(function(p){p.classList.remove('active')});
  document.getElementById('sidebar').classList.add('hidden');
  document.getElementById('sidebar-menu').innerHTML='';
  document.getElementById('display-username').textContent='未登录';
  document.getElementById('display-role').textContent='';
  document.getElementById('notify-count').style.display='none';
  document.getElementById('role-selector').style.display='';
  document.querySelectorAll('.login-card-wrap').forEach(function(w){w.classList.remove('show')});
  var rf=document.getElementById('register-form'); if(rf) rf.classList.remove('show');
  document.getElementById('login-overlay').classList.add('show');
  Object.values(state.echarts).forEach(function(i){try{i.dispose()}catch(e){}});
  state.echarts={};
  if(state.bannerTimer) clearInterval(state.bannerTimer);
  document.getElementById('notify-dropdown').classList.remove('show');
}

// ============================================================
// NOTIFICATIONS
// ============================================================
function toggleNotify() {
  document.getElementById('notify-dropdown').classList.toggle('show');
}
function markAllRead() {
  document.querySelectorAll('#notify-list .n-item').forEach(function(i){i.classList.remove('unread')});
  document.getElementById('notify-count').style.display='none';
  showToast('全部标记为已读');
}

// ============================================================
// NAVIGATION
// ============================================================
function renderSidebar(role) {
  var menu = sidebarMenus[role]||[];
  var sb=document.getElementById('sidebar');
  var mc=document.getElementById('sidebar-menu');
  if(!menu.length){sb.classList.add('hidden')}
  else{
    sb.classList.remove('hidden');
    mc.innerHTML='<div class="sidebar-group">导航菜单</div>'+menu.map(function(m){
      return '<div class="sidebar-item" data-page="'+m.page+'" onclick="navigateTo(\''+m.page+'\')"><span class="icon">'+m.icon+'</span>'+m.label+'</div>';
    }).join('');
  }
}
function navigateTo(page) {
  state.currentPage=page;
  document.querySelectorAll('.page').forEach(function(p){p.classList.remove('active')});
  var t=document.querySelector('.page[data-page="'+page+'"]');
  if(t) t.classList.add('active');
  document.querySelectorAll('.sidebar-item').forEach(function(i){i.classList.toggle('active',i.dataset.page===page)});
  if(page==='profile') fillProfile();
  if(page==='school-dashboard') setTimeout(initDashboard,400);
  if(page==='student-activity-list'||page==='teacher-activity-list') initBanner();
  if(page!=='school-dashboard'){
    Object.values(state.echarts).forEach(function(i){try{i.dispose()}catch(e){}});
    state.echarts={};
  }
}
function fillProfile() {
  var d=profileData[state.currentRole];
  if(!d) return;
  document.getElementById('profile-avatar').textContent=d.avatar;
  document.getElementById('profile-name').textContent=d.name;
  document.getElementById('profile-role-text').textContent=roleLabels[state.currentRole]+' · 新疆大学';
  document.getElementById('profile-info').innerHTML=d.fields.map(function(f){
    return '<div class="info-label">'+f[0]+'</div><div class="info-value">'+f[1]+'</div>';
  }).join('');
}

// ============================================================
// WITHDRAWAL
// ============================================================
function withdrawEnrollment(btn,name) {
  if(confirm('确定退出「'+name+'」的报名吗？')) {
    var row=btn.closest('tr');
    row.querySelector('td:nth-child(5)').innerHTML='<span class="badge badge-danger">已退出</span>';
    row.querySelector('td:last-child').innerHTML='<span style="font-size:12px;color:var(--text-muted)">已退出</span>';
    showToast('已退出「'+name+'」');
  }
}

// ============================================================
// BANNER
// ============================================================
function initBanner() {
  if(state.bannerTimer) clearInterval(state.bannerTimer);
  state.bannerIdx=0;
  state.bannerTimer=setInterval(function(){
    document.querySelectorAll('.pc-banner').forEach(function(b){
      var dots=b.querySelectorAll('.dot'); var total=dots.length||3;
      state.bannerIdx=(state.bannerIdx+1)%total;
      var s=b.querySelector('.slides');
      if(s) s.style.transform='translateX(-'+(state.bannerIdx*100)+'%)';
      dots.forEach(function(d,i){d.classList.toggle('active',i===state.bannerIdx)});
    });
  },4000);
}

// ============================================================
// HELPERS
// ============================================================
function switchTab(el){el.parentElement.querySelectorAll('.tab').forEach(function(t){t.classList.remove('active')});el.classList.add('active')}
function openModal(id){document.getElementById(id).classList.add('show')}
function closeModal(id){document.getElementById(id).classList.remove('show')}
function rateStars(e){
  if(!e.target.classList.contains('star')) return;
  var v=parseInt(e.target.dataset.v);
  e.target.parentElement.querySelectorAll('.star').forEach(function(s){s.classList.toggle('active',parseInt(s.dataset.v)<=v)});
}
function showToast(msg){
  var t=document.createElement('div');t.className='toast toast-success';t.innerHTML='✅ '+msg;
  document.body.appendChild(t);
  setTimeout(function(){t.style.opacity='0';t.style.transition='opacity .3s';setTimeout(function(){t.remove()},300)},2500);
}
function showSchoolSuggest(inp){
  var sd=inp.parentElement.querySelector('.school-suggest');
  if(sd) sd.style.display=inp.value.trim().length>0?'block':'none';
}
function selectSchool(name){
  var inp=document.getElementById('school-input');
  if(inp){inp.value=name;var sd=inp.parentElement.querySelector('.school-suggest');if(sd)sd.style.display='none'}
}
document.addEventListener('click',function(e){
  if(e.target.classList.contains('modal-overlay')&&e.target.classList.contains('show')) e.target.classList.remove('show');
  if(!e.target.closest('.has-suggest')) document.querySelectorAll('.school-suggest').forEach(function(s){s.style.display='none'});
  if(!e.target.closest('.notify-wrap')&&!e.target.closest('#notify-dropdown')) document.getElementById('notify-dropdown').classList.remove('show');
});

// ============================================================
// DASHBOARD
// ============================================================
function initDashboard(){
  var t=document.getElementById('chart-trend');
  if(t){var c=echarts.init(t);state.echarts.trend=c;c.setOption({tooltip:{trigger:'axis'},grid:{left:50,right:20,top:10,bottom:30},xAxis:{type:'category',data:['1月','2月','3月','4月','5月','6月','7月'],axisLabel:{fontSize:11,color:'#6b7280'}},yAxis:{type:'value',axisLabel:{fontSize:11,color:'#6b7280'}},series:[{name:'学生',type:'line',data:[25,15,80,120,60,40,95],smooth:true,lineStyle:{color:'#1a56db',width:2},itemStyle:{color:'#1a56db'},areaStyle:{color:'rgba(26,86,219,.08)'},symbol:'circle',symbolSize:6},{name:'教师',type:'line',data:[5,3,10,15,8,5,12],smooth:true,lineStyle:{color:'#10b981',width:2},itemStyle:{color:'#10b981'},areaStyle:{color:'rgba(16,185,129,.08)'},symbol:'circle',symbolSize:6}],legend:{data:['学生','教师'],bottom:0,textStyle:{fontSize:11}}});}
  var cl=document.getElementById('chart-college');
  if(cl){var c=echarts.init(cl);state.echarts.college=c;c.setOption({tooltip:{trigger:'axis'},grid:{left:100,right:30,top:10,bottom:20},xAxis:{type:'value',axisLabel:{fontSize:11,color:'#6b7280'}},yAxis:{type:'category',axisLabel:{fontSize:11,color:'#6b7280'},data:['计算机学院','数学学院','信息学院','物理学院','化学学院','生科学院']},series:[{type:'bar',data:[145,98,86,60,45,32],barMaxWidth:18,label:{show:true,position:'right',fontSize:11,color:'#6b7280'},itemStyle:{borderRadius:[0,4,4,0],color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#1a56db'},{offset:1,color:'#818cf8'}])}}]});}
  var r=document.getElementById('chart-rating');
  if(r){var c=echarts.init(r);state.echarts.rating=c;c.setOption({tooltip:{trigger:'item',formatter:'{b}: {c}条 ({d}%)'},series:[{type:'pie',radius:['50%','72%'],center:['50%','55%'],label:{fontSize:11},data:[{value:52,name:'★★★★★',itemStyle:{color:'#10b981'}},{value:28,name:'★★★★☆',itemStyle:{color:'#34d399'}},{value:10,name:'★★★☆☆',itemStyle:{color:'#f59e0b'}},{value:3,name:'★★☆☆☆',itemStyle:{color:'#f97316'}},{value:1,name:'★☆☆☆☆',itemStyle:{color:'#ef4444'}}]}]});}
  var tp=document.getElementById('chart-type');
  if(tp){var c=echarts.init(tp);state.echarts.type=c;c.setOption({tooltip:{trigger:'item',formatter:'{b}: {c}个 ({d}%)'},series:[{type:'pie',radius:['50%','72%'],center:['50%','55%'],label:{fontSize:11},data:[{value:5,name:'线下活动',itemStyle:{color:'#1a56db'}},{value:2,name:'线上活动',itemStyle:{color:'#6366f1'}},{value:1,name:'校内活动',itemStyle:{color:'#10b981'}}]}]});}
}
window.addEventListener('resize',function(){Object.values(state.echarts).forEach(function(i){try{i.resize()}catch(e){}})});
</script>'''

content = content[:script_start] + new_js + content[script_end:]
with open('e:/code-review-bot/prototype/index.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done - JS replaced')
