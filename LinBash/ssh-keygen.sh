yum -y install sshpass

ssh-keygen


for i in {1..3}; do sshpass -p 'Zjs0202520'  ssh-copy-id -o StrictHostKeyChecking=no root@192.168.128.12$i; done

for i in {1..3}; do sshpass -p 'Zjs0202520' ssh-copy-id -o StrictHostKeyChecking=no ecs$i.xckt.com.; done

