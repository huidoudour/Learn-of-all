from collections import Counter
text=input("请输入一句英文语句：\n")
swapped=text.swapcase()
letters=[letters.strip(",.") for letters in text.split()]
letters_feq=Counter(letters)
for letter, freq in letters_feq.items():
    print(f"{letter} {freq}")
