#include<iostream>
#include<math.h>
 using namespace std;
  int main (){
  	 int a;
  	  cout << " nhap so ngay vao ";
  	   cin >>a;
  	   
  	   int t = (a%365)/30;
  	   int n = a - (a/365)*365-t*30;
  	    cout << " so nam  " << a / 365 << endl;
  	    cout << " thang  " << t << endl;
  	    cout <<"ngay  " << n;
  	    return 0;
  }
