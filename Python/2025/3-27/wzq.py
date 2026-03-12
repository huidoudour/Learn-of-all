def print_gobang_board(size):

    print("  ", end="")
    for i in range(size):
        print(f"{i:2d}", end="")
    print()

    for i in range(size):
        print(f"{i:2d}", end="")
        for j in range(size):
            if i == size - 1:
                if j == size - 1:
                    print("└─", end="")
                else:
                    print("┴─", end="")
            elif j == size - 1:
                if i == 0:
                    print("┐ ", end="")
                else:
                    print("┤ ", end="")
            elif i == 0:
                if j == 0:
                    print("┌─", end="")
                else:
                    print("┬─", end="")
            elif j == 0:
                if i == 0:
                    print("┌─", end="")
                else:
                    print("├─", end="")
            else:
                print("┼─", end="")
        print()

board_size = 15
print_gobang_board(board_size)
