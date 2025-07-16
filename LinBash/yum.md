```
Loaded plugins: fastestmirror
Determining fastest mirrors
localyum                                                                                                                                                                                | 3.6 kB  00:00:00     
file:///root/package/repodata/repomd.xml: [Errno 14] curl#37 - "Couldn't open file /root/package/repodata/repomd.xml"
Trying other mirror.


 One of the configured repositories failed (offline),
 and yum doesn't have enough cached data to continue. At this point the only
 safe thing yum can do is fail. There are a few ways to work "fix" this:

     1. Contact the upstream for the repository and get them to fix the problem.

     2. Reconfigure the baseurl/etc. for the repository, to point to a working
        upstream. This is most often useful if you are using a newer
        distribution release than is supported by the repository (and the
        packages for the previous distribution release still work).

     3. Run the command with the repository temporarily disabled
            yum --disablerepo=offline ...

     4. Disable the repository permanently, so yum won't use it by default. Yum
        will then just ignore the repository until you permanently enable it
        again or use --enablerepo for temporary usage:

            yum-config-manager --disable offline
        or
            subscription-manager repos --disable=offline

     5. Configure the failing repository to be skipped, if it is unavailable.
        Note that yum will try to contact the repo. when it runs most commands,
        so will have to try and fail each time (and thus. yum will be be much
        slower). If it is a very temporary problem though, this is often a nice
        compromise:

            yum-config-manager --save --setopt=offline.skip_if_unavailable=true

failure: repodata/repomd.xml from offline: [Errno 256] No more mirrors to try.
file:///root/package/repodata/repomd.xml: [Errno 14] curl#37 - "Couldn't open file /root/package/repodata/repomd.xml"
[root@appsrv yum.repos.d]# 

```

```
已加载插件：fastestmirror
确定最快的镜像
localyum | 3.6 kB 00:00:00
file:///root/package/repodata/repomd.xml：[Errno 14] curl#37 - “无法打开文件 /root/package/repodata/repomd.xml”
尝试其他镜像。

其中一个配置的存储库失败（离线），
并且 yum 没有足够的缓存数据来继续。此时 yum 唯一能做的安全事情就是失败。有几种方法可以“修复”此问题：

1. 联系存储库的上游并让他们修复问题。

2. 重新配置存储库的 baseurl/etc。以指向有效的上游。如果您使用的是比存储库支持的较新的发行版（并且上一个发行版的软件包仍然有效），这通常非常有用。

3. 在暂时禁用存储库的情况下运行命令
yum --disablerepo=offline ...

4. 永久禁用存储库，这样 yum 默认不会使用它。Yum
然后会忽略存储库，直到您再次永久启用它
或使用 --enablerepo 进行临时使用：

yum-config-manager --disable offline
或
subscription-manager repos --disable=offline

5. 如果存储库不可用，请配置要跳过的存储库。
请注意，yum 在运行大多数命令时都会尝试联系存储库。
因此每次都必须尝试并失败（因此 yum 会慢得多）。不过，如果这只是一个临时问题，那么这通常是一个很好的折衷方案：

yum-config-manager --save --setopt=offline.skip_if_unavailable=true

失败：离线的 repodata/repomd.xml：[Errno 256] 没有更多镜像可供尝试。
file:///root/package/repodata/repomd.xml：[Errno 14] curl#37 - “无法打开文件 /root/package/repodata/repomd.xml”
[root@appsrv yum.repos.d]#
```

