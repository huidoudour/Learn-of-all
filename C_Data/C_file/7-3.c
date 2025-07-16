#include <stdio.h>

int main(void) {
int i, j, n = 3;
    for (i = 0, j = 0; j++ < n || (puts(""), j = 1, ++i < n);
    printf("%d\t", ((n-1)*3/2 + j - i) % n * n + ((n*5-1)/2 - i - j) % n + 1));
    return 0;
}