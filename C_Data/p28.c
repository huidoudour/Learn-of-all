#define _CRT_SECURE_NO_WARNING
#include <stdio.h>
#include <stdlib.h>
int main() {
	int person[100];
	int i,j;
	int arrayLen;
	int start,N;
	int deleNum;
	int name,M;
	printf("请输入圆桌上人的总数：");
	scanf("%d",&arrayLen);
	printf("\n");
	printf("请输入每个人的信息(整数)：\n");
	for(i=0;i<arrayLen;i++)
	{
		scanf("%d",&name);
		person[i]=name;
	}
	printf("你输入的数据的顺序为：\n");
	for(i=0;i<arrayLen-1;i++)
	{
		printf("%d==>",person[i]);
	}
	printf("%d\n",person[arrayLen-1]);
	printf("你打算从第几个人开始报数？");
	scanf("%d",&start);
	start=start-1;
	printf("请输入报数为多少时出圈？");
	scanf("%d",&N);
	printf("\n");
	M=arrayLen;
	printf("程序运行后，出列人的顺序为：\n\n");
	for(i=0;i<M;i++)
	{
		if (arrayLen==1)
		{
			printf("%d",person[0]);
		}
		else
		{
			deleNum=(start+N-1)%arrayLen;
			printf("%d==>",person[deleNum]);
			for(j=deleNum;j<arrayLen;j++)
			{
				person[j]=person[j+1];
			}
			start=deleNum;
			arrayLen=arrayLen-1;

		}
	}
	printf("\n\n");

	system("pause");
	return 0;
}
