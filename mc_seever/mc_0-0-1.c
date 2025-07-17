#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#define DEFAULT_JAR "server.jar"
#define MIN_MEMORY "512M"
#define MAX_MEMORY "2048M"
#define BUFFER_SIZE 1024

// 启动服务器函数
void start_server(const char *jar_file, const char *min_mem, const char *max_mem, bool nogui) {
    pid_t pid = fork();
    
    if (pid == 0) {
        // 子进程
        char command[BUFFER_SIZE];
        snprintf(command, BUFFER_SIZE, "java -Xms%s -Xmx%s -jar %s %s", 
                min_mem, max_mem, jar_file, nogui ? "-nogui" : "");
        
        printf("启动命令: %s\n", command);
        
        // 执行命令
        execlp("java", "java", "-Xms", min_mem, "-Xmx", max_mem, "-jar", jar_file, nogui ? "-nogui" : NULL);
        
        // 如果execlp失败
        perror("无法启动服务器");
        exit(EXIT_FAILURE);
    } else if (pid < 0) {
        // fork失败
        perror("无法创建子进程");
    } else {
        // 父进程
        printf("服务器已启动 (PID: %d)\n", pid);
    }
}

// 显示帮助信息
void show_help() {
    printf("\nMinecraft 服务器管理工具\n");
    printf("用法:\n");
    printf("  start [jar文件] [最小内存] [最大内存] - 启动服务器\n");
    printf("  stop                                  - 停止服务器\n");
    printf("  status                                - 查看服务器状态\n");
    printf("  help                                  - 显示帮助信息\n");
    printf("  exit                                  - 退出管理工具\n");
    printf("\n默认值:\n");
    printf("  jar文件: %s\n", DEFAULT_JAR);
    printf("  最小内存: %s\n", MIN_MEMORY);
    printf("  最大内存: %s\n", MAX_MEMORY);
}

int main() {
    char input[BUFFER_SIZE];
    char *command;
    char *arg1, *arg2, *arg3;
    pid_t server_pid = 0;
    
    printf("Minecraft 服务器管理工具 - 输入 'help' 获取帮助\n");
    
    while (1) {
        printf("> ");
        fgets(input, BUFFER_SIZE, stdin);
        input[strcspn(input, "\n")] = '\0'; // 移除换行符
        
        command = strtok(input, " ");
        if (command == NULL) continue;
        
        if (strcmp(command, "start") == 0) {
            // 解析参数
            arg1 = strtok(NULL, " ");
            arg2 = strtok(NULL, " ");
            arg3 = strtok(NULL, " ");
            
            const char *jar = arg1 ? arg1 : DEFAULT_JAR;
            const char *min_mem = arg2 ? arg2 : MIN_MEMORY;
            const char *max_mem = arg3 ? arg3 : MAX_MEMORY;
            
            start_server(jar, min_mem, max_mem, true);
        } 
        else if (strcmp(command, "stop") == 0) {
            if (server_pid > 0) {
                printf("停止服务器 (PID: %d)\n", server_pid);
                kill(server_pid, SIGTERM);
                server_pid = 0;
            } else {
                printf("没有运行的服务器实例\n");
            }
        }
        else if (strcmp(command, "status") == 0) {
            if (server_pid > 0) {
                printf("服务器正在运行 (PID: %d)\n", server_pid);
            } else {
                printf("服务器未运行\n");
            }
        }
        else if (strcmp(command, "help") == 0) {
            show_help();
        }
        else if (strcmp(command, "exit") == 0) {
            printf("退出管理工具\n");
            break;
        }
        else {
            printf("未知命令: %s\n", command);
            printf("输入 'help' 获取帮助\n");
        }
    }
    
    return 0;
}
