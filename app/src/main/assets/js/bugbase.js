function print(){
    window.android.print("bbbbbbbb");//重载默认HTML系统函数
}
function print(str){
    window.android.print(str);//传入打印数据
}