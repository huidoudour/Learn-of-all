#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <stdlib.h>

void safe_flush(FILE *stream) {
    int c;
    while ((c = getchar())!= '\n' && c!= EOF);
}

int esd1() {
    system("cls");
    printf("即将启动\n");
    system("java -server -Xms1024M -Xmx4096M -jar fabric-server-launch.jar -nogui");
    return 0;
}

int esd2() {
    system("cls");
    printf("qwq\n");
    return 0;
}

int esd3() {
    system("cls");
    printf("退出？\n需要按任意键确认");
    system("pause");
    system("exit");
    return 0;
}

int main() {
    int num;
    while (1) {
        system("cls");
        printf("输入1、2、3选择需要进行的操作\n");
        printf("“1”是启动服务器\n“2”是“=￣ω￣=”\n“3”是退出该程序\n");
        printf("请输入1或2或3以执行程序分命令\n");

        if (scanf("%d", &num)!= 1) {
            printf("输入错误，请重新输入。\n");
            safe_flush(stdin);
            system("pause");
            continue;
        }

        switch (num) {
            case 1:
                safe_flush(stdin);
                esd1();
                system("pause");
                break;
            case 2:
                safe_flush(stdin);
                esd2();
                system("pause");
                break;
            case 3:
                safe_flush(stdin);
                return esd3();
            default:
                printf("输入错误，请重新输入。\n");
                safe_flush(stdin);
                system("pause");
        }
    }
    return 0;
}