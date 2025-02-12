package cn.hellohao.utils;

import cn.hellohao.config.GlobalConstant;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Hellohao
 * @version 1.0
 * @date 2019/11/29 14:32
 */
@Configuration
public class FirstRun implements InitializingBean {

    @Value("${spring.datasource.username}")
    private String jdbcusername;

    @Value("${spring.datasource.password}")
    private String jdbcpass;

    @Value("${spring.datasource.url}")
    private String jdbcurl;

    @Override
    public void afterPropertiesSet() {
        isWindows();
        RunSqlScript.USERNAME = jdbcusername;
        RunSqlScript.PASSWORD = jdbcpass;
        RunSqlScript.DBURL = jdbcurl;

        Integer dataBaseName = RunSqlScript.RunSelectCount("SELECT count(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'tbed'");
        if(dataBaseName<1){
            Print.Normal("无法找到指定数据库，请先创建'tbed'数据库，并导入表结构后再试。");
            System.exit(1);
        }

        Print.Normal("正在校验数据库参数...");
        RunSqlScript.RunInsert(dynamic);
        RunSqlScript.RunInsert(compressed);

        String uid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        RunSqlScript.RunInsert("update user set uid='"+uid+"' where id = 1");

        Integer ret1 = RunSqlScript.RunSelectCount(sql1);
        if(ret1==0){
            Print.Normal("In execution...");
            RunSqlScript.RunInsert(sql2);
        }else{
            if(ret1>0){
                Print.Normal("Stage 1");
            }else{
                Print.Normal("Mysql 报了一个错");
            }
        }
        Integer imgandalbum = RunSqlScript.RunSelectCount(judgeTable+"'imgandalbum'");
        if(imgandalbum==0){
            RunSqlScript.RunInsert(sql3);
            Print.Normal("Stage 2");
        }
        Integer album = RunSqlScript.RunSelectCount(judgeTable+"'album'");
        if(album==0){
            RunSqlScript.RunInsert(sql4);
            Print.Normal("Stage 3");
        }
        Integer ret2 = RunSqlScript.RunSelectCount(judgeTable+"'imgdata' and column_name = 'explains'");
        if(ret2==0){
            RunSqlScript.RunInsert(sql6);
            Print.Normal("Stage 4");
        }
        Integer ret3 = RunSqlScript.RunSelectCount(judgeTable+"'album' and column_name = 'userid'");
        if(ret3==0){
            RunSqlScript.RunInsert(sql7);
            Print.Normal("Stage 5");
        }
        Integer ret4 = RunSqlScript.RunSelectCount(judgeTable+"'imgdata' and column_name = 'md5key'");
        if(ret4==0){
            RunSqlScript.RunInsert(sql8);
            Print.Normal("Stage 6");
        }
        Integer ret5 = RunSqlScript.RunSelectCount(judgeTable+"'config' and column_name = 'theme'");
        if(ret5==0){
            RunSqlScript.RunInsert(sql9);
            Print.Normal("Stage 7");
        }
        Integer ret6 = RunSqlScript.RunSelectCount(judgeTable+" 'user' and column_name = 'token'");
        if(ret6==0){
            RunSqlScript.RunInsert(sql12);
            Print.Normal("Add user.token");
        }
        Integer isappclient = RunSqlScript.RunSelectCount(isTableName+"'appclient'");
        if(isappclient==0){
            Integer integer = RunSqlScript.RunInsert(createAppclient);
            RunSqlScript.RunInsert(instartAppclient);
            Print.Normal("Add table.appclient");
        }
        Integer ret7 = RunSqlScript.RunSelectCount(judgeTable+" 'imgdata' and column_name = 'idname'");
        if(ret7==0){
            RunSqlScript.RunInsert(sql11);
            Print.Normal("Add imgdata.idname");
        }

        RunSqlScript.RunInsert("alter table `config` modify column `explain` text,modify column `links` text,modify column `notice` text,modify column `baidu` text");

        RunSqlScript.RunInsert(inddx_md5key);
        RunSqlScript.RunInsert("UPDATE `keys` SET `Endpoint` = '0' WHERE `id` = 8");
        Print.Normal("Stage success");

        clears();
    }

    private String isTableName = "SELECT count(table_name) FROM information_schema.TABLES WHERE TABLE_SCHEMA='tbed' and table_name =";
    private String judgeTable = "select count(*) from information_schema.columns where TABLE_SCHEMA='tbed' and table_name = ";
    //创建blacklist  2019-11-29
    private String sql1 = "select count(*) from information_schema.columns where table_name = 'uploadconfig' and column_name = 'blacklist'";
    private String sql2 = "alter table uploadconfig add blacklist varchar(500);";
    //创建imgandalbum和album 添加imgdata表字段explain 2019-12-20
    private String sql3 ="CREATE TABLE `imgandalbum`  (`imgname` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,`albumkey` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic";
    private String sql4 ="CREATE TABLE `album`  (`albumkey` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,`albumtitle` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,`createdate` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL, `password` varchar(5000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic";
    private String sql6 = "alter table imgdata add explains varchar(5000)";
    private String sql7 = "alter table album add userid int(10)";
    private String sql8 = "alter table imgdata add md5key varchar(5000)";
    private String sql9 = "ALTER TABLE config ADD theme int(4) DEFAULT '1' COMMENT '主题'  ";
    private String sql11 = "alter table `imgdata` add idname varchar(255) DEFAULT '未命名图像' ;";
    private String sql12 = "alter table tbed.user add `token` varchar(255)";
    private String createAppclient = "CREATE TABLE `appclient`  (`id` varchar(10) NOT NULL,`isuse` varchar(10) NOT NULL,`winpackurl` varchar(255) NULL DEFAULT NULL,`macpackurl` varchar(255) NULL DEFAULT NULL,`appname` varchar(20) NULL,`applogo` varchar(255) NULL,`appupdate` varchar(10) NOT NULL) ";
    private String instartAppclient = "INSERT INTO `appclient` VALUES ('app', 'on', NULL, NULL, 'Hellohao图像托管', 'https://hellohao.nos-eastchina1.126.net/TbedClient/app.png', '1.0.1');";
    private String inddx_md5key = "ALTER TABLE imgdata ADD INDEX index_md5key_url ( md5key,imgurl)";
    private String dynamic = "alter table imgdata row_format=dynamic";
    private String compressed = "alter table imgdata row_format=compressed";

    private void clears(){
        File file1 = new File(GlobalConstant.LOCPATH+File.separator+"hellohaotempimg");
        File file2 = new File(GlobalConstant.LOCPATH+File.separator+"hellohaotempwatermarimg");

        //判断目录有没有创建
        File file = new File(GlobalConstant.LOCPATH);
        if(!file.exists()){
            file.mkdirs();
            file1.mkdirs();
            file2.mkdirs();
        }else{
            if(!file1.exists()){
                file1.mkdirs();
            }else if(!file2.exists()){
                file2.mkdirs();
            }
        }
    }


    public boolean isWindows() {
        System.out.println("当前系统类型:"+System.getProperties().getProperty("os.name").toUpperCase());
        if(System.getProperties().getProperty("os.name").toUpperCase().contains("MAC")){
            GlobalConstant.SYSTYPE = "MAC";
            Properties props=System.getProperties();
            GlobalConstant.LOCPATH = props.getProperty("user.home")+File.separator+".HellohaoData";
        }
        return System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }

}
