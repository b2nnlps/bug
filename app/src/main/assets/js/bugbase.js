function print(){
    var data=document.getElementsByTagName('html')[0].innerHTML;
    window.android.print(data);//重载默认HTML系统函数
    console.log("打印网页内容");
}
function print(str){
    window.android.print(str);//传入打印数据
}

function bugCom(){
    com=getQueryString("bugCom");
    if(com=="login") bugLogin();
}


function getQueryString(name) { //获取get传过来的用户信息
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return "";
}

console.log("七星虫JS执行中");
bugCom();