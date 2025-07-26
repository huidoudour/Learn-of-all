# Ansible服务

```
OpenEuler系统上配置以下的详细过程
1.	在ecs3安装Ansible服务，添加ecs1/ecs2为受控节点。
2.	 使用用户列表user.list.yml，并将它保存到/home/student/ansible，用户密码来自于/home/student/ansible/locker.yml文件。 
3.	创建名为/opt/users.yml 的playbook，从而按以下所述创建用户帐户：职位描述为 developer 的用户应当：在 dev 和 test 主机组中的受管节点上创建 从 pw_developer 变量分配密码 是附加组 student 的成员 职位描述为 manager 的用户应当： 在 prod 主机组中的受管节点上创建 从 pw_manager 变量分配密码 是附加组 opsmgr 的成员 。
4.	密码应采用 SHA512 哈希格式。
5.	 playbook 使用在其他位置创建的库密码文件 /opt/secret.txt 正常运行。
```

------

好的，下面是如何在 ECS3 上安装 Ansible 服务，并配置受控节点 ECS1 和 ECS2，然后创建所需的 playbook 以满足任务要求。

### 1. 在 ECS3 上安装 Ansible 服务

首先，在 ECS3 上安装 Ansible。假设 ECS3 运行的是基于 Red Hat 的系统（如 CentOS 或 RHEL），可以使用以下命令进行安装：

```bash
sudo yum install -y epel-release
sudo yum install -y ansible
```

### 2. 添加 ECS1 和 ECS2 为受控节点

在 ECS3 上编辑 Ansible 的主机清单文件 `/etc/ansible/hosts`，添加 ECS1 和 ECS2 为受控节点。假设 ECS1 和 ECS2 的 IP 地址分别是 `192.168.88.101` 和 `192.168.88.102`。

ini深色版本

```ini
[dev]
ecs1IP
ecs2IP

[test]
ecs1IP
ecs2IP

[prod]
ecs1IP
ecs2IP
```

### 3. 创建用户列表和密码文件

在 ECS3 上创建用户列表文件 `/home/student/ansible/user.list.yml` 和密码文件 `/home/student/ansible/locker.yml`。

#### 用户列表文件 `/home/student/ansible/user.list.yml`

```yaml
users:
  - name: alice
    role: developer
  - name: bob
    role: developer
  - name: charlie
    role: manager
```

#### 密码文件 `/home/student/ansible/locker.yml`

```bash
pw_developer: "developer_password"
pw_manager: "manager_password"
```

### 4. 创建 playbook `/opt/users.yml`

创建一个名为 `/opt/users.yml` 的 playbook，用于根据用户列表和密码文件创建用户账户，并设置相应的组和密码哈希。

```yaml
详见 6 
```

### 5. 创建密码库文件 `/opt/secret.txt`

创建一个密码库文件 `/opt/secret.txt`，用于存储密码。这个文件将用于在 playbook 中引用密码。

```bash
echo "developer_password" > /opt/secret.txt
echo "manager_password" >> /opt/secret.txt
```

### 6. 修改 playbook 以使用密码库文件

修改 playbook `/opt/users.yml`，使其从 `/opt/secret.txt` 中读取密码。我们可以使用 `lookup` 插件来读取密码文件。

```yml
---
- name: Create users based on their roles
  hosts: all
  become: yes
  vars:
    pw_developer: "{{ lookup('file', '/opt/secret.txt').split('\n')[0] }}"
    pw_manager: "{{ lookup('file', '/opt/secret.txt').split('\n')[1] }}"
  tasks:
    - name: Include user list
      include_vars:
        file: /home/student/ansible/user.list.yml
        name: user_list

    - name: Ensure required groups exist
      group:
        name: "{{ item }}"
        state: present
      loop:
        - student
        - opsmgr

    - name: Debug user list
      debug:
        var: user_list

    - name: Create developer users
      user:
        name: "{{ item.name }}"
        password: "{{ pw_developer | password_hash('sha512') }}"
        groups: student
        state: present
      loop: "{{ user_list.users }}"
      when: item.role == 'developer' and (inventory_hostname in groups['dev'] or inventory_hostname in groups['test'])

    - name: Create manager users
      user:
        name: "{{ item.name }}"
        password: "{{ pw_manager | password_hash('sha512') }}"
        groups: opsmgr
        state: present
      loop: "{{ user_list.users }}"
      when: item.role == 'manager' and inventory_hostname in groups['prod']
```

### 7. 运行 playbook

最后，运行 playbook 以创建用户账户。

```bash
ansible-playbook /opt/users.yml
```



验证方法

```bash
cat /etc/passwd | grep -E 'alice|bob|charlie'
id alice
id bob
id charlie
```

