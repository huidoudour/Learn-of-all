#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <windows.h>

#define DEFAULT_JAR "server.jar"
#define MIN_MEMORY "512M"
#define MAX_MEMORY "2048M"
#define BUFFER_SIZE 1024

typedef struct {
    PROCESS_INFORMATION pi;
    HANDLE hChildStd_IN_Rd;  // 子进程输入读取端
    HANDLE hChildStd_IN_Wr;  // 子进程输入写入端
    HANDLE hChildStd_OUT_Rd; // 子进程输出读取端
    HANDLE hChildStd_OUT_Wr; // 子进程输出写入端
} ServerProcess;

static ServerProcess server = {0};

// 创建管道与服务器通信
bool create_pipes() {
    SECURITY_ATTRIBUTES sa = {
        sizeof(SECURITY_ATTRIBUTES),
        NULL,
        TRUE  // 句柄可继承
    };

    // 创建子进程输入管道
    if (!CreatePipe(&server.hChildStd_IN_Rd, &server.hChildStd_IN_Wr, &sa, 0)) {
        return false;
    }

    // 创建子进程输出管道
    if (!CreatePipe(&server.hChildStd_OUT_Rd, &server.hChildStd_OUT_Wr, &sa, 0)) {
        CloseHandle(server.hChildStd_IN_Rd);
        CloseHandle(server.hChildStd_IN_Wr);
        return false;
    }

    return true;
}

// 关闭管道
void close_pipes() {
    if (server.hChildStd_IN_Rd) CloseHandle(server.hChildStd_IN_Rd);
    if (server.hChildStd_IN_Wr) CloseHandle(server.hChildStd_IN_Wr);
    if (server.hChildStd_OUT_Rd) CloseHandle(server.hChildStd_OUT_Rd);
    if (server.hChildStd_OUT_Wr) CloseHandle(server.hChildStd_OUT_Wr);

    server.hChildStd_IN_Rd = server.hChildStd_IN_Wr = NULL;
    server.hChildStd_OUT_Rd = server.hChildStd_OUT_Wr = NULL;
}

// 启动服务器函数
void start_server(const char *jar_file, const char *min_mem, const char *max_mem, bool nogui) {
    if (server.pi.hProcess) {
        printf("服务器已在运行中 (PID: %d)\n", server.pi.dwProcessId);
        return;
    }

    char command[BUFFER_SIZE];
    snprintf(command, BUFFER_SIZE, "java -Xms%s -Xmx%s -jar %s %s",
             min_mem, max_mem, jar_file, nogui ? "nogui" : "");

    printf("启动命令: %s\n", command);

    STARTUPINFO si = { sizeof(STARTUPINFO) };
    si.dwFlags = STARTF_USESTDHANDLES;

    // 创建管道
    if (!create_pipes()) {
        printf("创建管道失败! 错误代码: %d\n", GetLastError());
        return;
    }

    si.hStdInput = server.hChildStd_IN_Rd;
    si.hStdOutput = server.hChildStd_OUT_Wr;
    si.hStdError = server.hChildStd_OUT_Wr;

    // 创建进程
    if (!CreateProcess(
        NULL,
        command,
        NULL,
        NULL,
        TRUE,
        CREATE_NO_WINDOW,
        NULL,
        NULL,
        &si,
        &server.pi))
    {
        printf("无法启动服务器! 错误代码: %d\n", GetLastError());
        close_pipes();
        return;
    }

    printf("服务器已启动 (PID: %d)\n", server.pi.dwProcessId);

    // 关闭我们不使用的句柄端
    CloseHandle(server.hChildStd_IN_Rd);
    CloseHandle(server.hChildStd_OUT_Wr);
    server.hChildStd_IN_Rd = server.hChildStd_OUT_Wr = NULL;
}

// 停止服务器函数
void stop_server() {
    if (!server.pi.hProcess) {
        printf("没有运行的服务器实例\n");
        return;
    }

    printf("正在停止服务器 (PID: %d)...\n", server.pi.dwProcessId);

    // 向服务器发送停止命令
    DWORD bytesWritten;
    const char* stop_cmd = "stop\n";
    WriteFile(server.hChildStd_IN_Wr, stop_cmd, strlen(stop_cmd), &bytesWritten, NULL);

    // 等待服务器正常关闭
    if (WaitForSingleObject(server.pi.hProcess, 10000) == WAIT_TIMEOUT) {
        printf("服务器未响应，强制终止...\n");
        TerminateProcess(server.pi.hProcess, 0);
    }

    // 关闭句柄
    CloseHandle(server.pi.hProcess);
    CloseHandle(server.pi.hThread);
    close_pipes();

    memset(&server, 0, sizeof(ServerProcess));
    printf("服务器已停止\n");
}

// 检查服务器状态
void check_server_status() {
    if (server.pi.hProcess) {
        DWORD exitCode;
        GetExitCodeProcess(server.pi.hProcess, &exitCode);

        if (exitCode == STILL_ACTIVE) {
            printf("服务器正在运行 (PID: %d)\n", server.pi.dwProcessId);

            // 读取服务器输出
            char buffer[BUFFER_SIZE];
            DWORD bytesRead;
            PeekNamedPipe(server.hChildStd_OUT_Rd, NULL, 0, NULL, &bytesRead, NULL);
            if (bytesRead > 0) {
                ReadFile(server.hChildStd_OUT_Rd, buffer, BUFFER_SIZE-1, &bytesRead, NULL);
                buffer[bytesRead] = '\0';
                printf("服务器最新输出:\n%s\n", buffer);
            }
        } else {
            printf("服务器已停止 (退出代码: %d)\n", exitCode);
            CloseHandle(server.pi.hProcess);
            CloseHandle(server.pi.hThread);
            close_pipes();
            memset(&server, 0, sizeof(ServerProcess));
        }
    } else {
        printf("服务器未运行\n");
    }
}

// 向服务器发送命令
void send_server_command(const char* cmd) {
    if (!server.pi.hProcess) {
        printf("服务器未运行，无法发送命令\n");
        return;
    }

    DWORD bytesWritten;
    char full_cmd[BUFFER_SIZE];
    snprintf(full_cmd, BUFFER_SIZE, "%s\n", cmd);

    if (!WriteFile(server.hChildStd_IN_Wr, full_cmd, strlen(full_cmd), &bytesWritten, NULL)) {
        printf("发送命令失败! 错误代码: %d\n", GetLastError());
    } else {
        printf("已发送命令: %s", full_cmd);
    }
}

// 显示帮助信息
void show_help() {
    printf("\nMinecraft 服务器管理工具 (Windows 版本)\n");
    printf("管理命令:\n");
    printf("  start [jar文件] [最小内存] [最大内存] - 启动服务器\n");
    printf("  stop                                  - 停止服务器\n");
    printf("  status                                - 查看服务器状态\n");
    printf("  cmd <服务器命令>                      - 向服务器发送命令\n");
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

    printf("Minecraft 服务器管理工具 (Windows) - 输入 'help' 获取帮助\n");

    while (1) {
        printf("> ");
        if (fgets(input, BUFFER_SIZE, stdin) == NULL) break;
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
            stop_server();
        }
        else if (strcmp(command, "status") == 0) {
            check_server_status();
        }
        else if (strcmp(command, "cmd") == 0) {
            // 获取剩余部分作为命令
            char *server_cmd = strtok(NULL, "");
            if (server_cmd) {
                send_server_command(server_cmd);
            } else {
                printf("请输入要发送的服务器命令\n");
            }
        }
        else if (strcmp(command, "help") == 0) {
            show_help();
        }
        else if (strcmp(command, "exit") == 0) {
            if (server.pi.hProcess) {
                printf("警告: 服务器仍在运行! 是否要停止服务器? (y/n) ");
                fgets(input, BUFFER_SIZE, stdin);
                if (input[0] == 'y' || input[0] == 'Y') {
                    stop_server();
                }
            }
            printf("退出管理工具\n");
            break;
        }
        else {
            printf("未知命令: %s\n", command);
            printf("输入 'help' 获取帮助\n");
        }
    }

    // 清理资源
    if (server.pi.hProcess) {
        stop_server();
    }

    return 0;
}
