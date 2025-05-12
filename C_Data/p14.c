#define _CRT_SECURE_NO_WARNING
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
typedef struct {
	long id;
	char pwd[6];
	int pms;

}user;

user users[50];
void init(int n)
{
	user *p;
	int i;
	p=users;
	printf("请管理员输入教师信息：\n");
	for (i = 0; i < n; i++ )
		scanf("%ld%s%d",&p[i].id,p[i].pwd,&p[i].pms);
}

int main ()
{
	long tid;
	char tpwd[6];
	int tpms;
	int i,n;
	printf("请输入教师数量：");
	scanf("%d",&n);
	init(n);
	printf("请你输入账号、密码和权限：");
	scanf("%ld%s%d",&tid,tpwd,&tpms);
	for (i = 0; i < n; i++)
	{
		if (tid==users[i].id&&strcmp(tpwd,users[i].pwd)==0)
		{
			if (tpms==1)
				printf("教学督导\n");
			else
				printf("普通教师\n");
			break;
		}
	}
	if (i==n)
		printf("账号或密码错误，登入失败！\n");
	system("pause");
}