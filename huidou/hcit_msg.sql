create database hcit_msg;
use hcit_msg;
create table comment (
	id int unsigned not null primary key auto_increment,
    date datetime not null,
    poster varchar(20) not null,
    comment text not null,
    replay text not null,
    mail varchar(60) not null,
    ip varchar(15) not null
)default charset=utf8;
use hcit_msg;
create table admin(
	id int unsigned not null primary key auto_increment,
    username varchar(20) not null,
    password varchar(32) not null,
    sakt char(4) not null
)default charset=utf8;