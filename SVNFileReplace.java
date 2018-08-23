import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SVNFileReplace {

    public static String exportPath = "";//tortoiseSVN导出的目录,src的上一级
    public static String projectPath = "";//项目根目录,WEB-INF的上一级
    public static String projectName = "";
    public static List<String> firstPrefixList = new ArrayList<String>();//带有源文件的文件夹的第一个前缀list
    public static List<String> prefixList = new ArrayList<String>();//导出文件夹中带有源文件的第一个前缀list

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while(!checkPath("项目的根目录（WEB-INF文件夹的上一级目录）",projectPath,"WEB-INF")){
            projectPath = scanner.next();
        }
        while(!checkPath("TortoiseSVN导出到的目录(比如src上级，即java文件夹，如果没有src文件夹，请新建一个空的src文件夹)",exportPath,"src")){
            exportPath = scanner.next();
        }
        projectName = new File(projectPath).getName();
        File checkWEBINF = new File(exportPath+"\\"+projectName+"\\WEB-INF\\classes");
        if(checkWEBINF.exists()) deleteAllFilesOfDir(checkWEBINF);
        getDirectory();
        for (int i = 0; i < prefixList.size(); i++) {
            getFile(new File(exportPath+prefixList.get(i)),prefixList.get(i));
            deleteAllFilesOfDir(new File(exportPath+prefixList.get(i)));
            String parent = new File(exportPath+prefixList.get(i)).getParent();
            File parentPath = new File(parent);
            deleteParent(parentPath);
        }
    }

    //检查路径
    private static boolean checkPath(String name,String path,String folder){
        Boolean flag = false;
        if("".equals(path)||path==null) {
            System.out.println("请输入" + name + ":");
            return flag;
        }
        if(!new File(path).exists()){
            System.out.println(name+"不存在，请重新输入:");
            return flag;
        }
        File flist[] = new File(path).listFiles();
        for (File f : flist) {
            if(f.getName().endsWith(folder))
                flag = true;
        }
        if(!flag){
            System.out.println(name+"不正确，请重新输入:");
        }
        return flag;
    }

    //得到编译源路径
    private static void getDirectory(){
        File flist[] = new File(projectPath+"\\WEB-INF\\classes").listFiles();
        for (File f : flist) {
            if(f.isDirectory()) {
                String prefixName = f.getName();
                getRootPrefix(f,prefixName);
            }
        }
        getPrefix(new File(exportPath));
    }

    //得到所有作为编译源路径的头
    private static void getRootPrefix(File file,String prefixName){
        File flist[] = file.listFiles();
        for (File f : flist) {
            if (f.isDirectory()) {
                getRootPrefix(f,prefixName);
            } else {
                if(f.getAbsolutePath().endsWith("class")){
                    if(!firstPrefixList.contains(prefixName))
                        firstPrefixList.add(prefixName);
                    return;
                }
            }
        }
    }

    //得到带有java目录中带有java文件的目录前缀
    private static void getPrefix(File file){
        File flist[] = file.listFiles();
        for (File f : flist) {
            if (f.isDirectory()) {
                for (int i = 0; i < firstPrefixList.size(); i++) {
                    if(f.getAbsolutePath().endsWith(firstPrefixList.get(i))){//当在遍历导出目录，进入名字属于头前缀list的文件夹的时候
                        String prefix = f.getParent().replace(exportPath,"");//将目录前缀去掉
                        Boolean exist = false;
                        String prefixTemp = prefix;
                        while (prefixTemp.contains("\\")){
                            int last = prefixTemp.lastIndexOf("\\");
                            if(last>0) prefixTemp = prefixTemp.substring(0, last);
                            else break;
                            if (prefixList.contains(prefixTemp)) exist = true;//在前缀list中存在就不加
                        }
                        if(!prefixList.contains(prefix)&&!exist) {
                            prefixList.add(prefix);
                        }
                    }
                }
                getPrefix(f);//迭代
            }
        }
    }

    //找到java类
    private static void getFile(File file,String prefix) {
        File flist[] = file.listFiles();
        if (flist == null || flist.length == 0) {
            System.out.println("tortoiseSVN导出目录下没有文件");
            return;
        }
        for (File f : flist) {
            if (f.isDirectory()) {
                getFile(f,prefix);
            } else {
                if(f.getAbsolutePath().endsWith("java")){
                    createPath(f,prefix);
                }
            }
        }
    }

    //生成文件路径并创建文件夹
    private static void createPath(File f,String prefix) {
        String sourceFileNameStr = f.getAbsolutePath();
        String desFileNameStr = f.getAbsolutePath();
        sourceFileNameStr = sourceFileNameStr.replace(exportPath + prefix,projectPath + "\\WEB-INF\\classes");
        desFileNameStr = desFileNameStr.replace(prefix,"\\" + projectName + "\\WEB-INF\\classes");
        sourceFileNameStr = sourceFileNameStr.replace(".java",".class");
        desFileNameStr = desFileNameStr.replace(".java",".class");

        File srcFile = new File(sourceFileNameStr);
        File desFile = new File(desFileNameStr);
        File desFilePath = new File(desFile.getParent());
        if(!desFilePath.exists()){
            desFilePath.mkdirs();
        }
        try {
            copyFile(srcFile, desFile);
            System.out.println(desFile.getName()+"复制完成");
        } catch (IOException e) {
            System.out.println(desFile.getName()+"复制失败");
            System.out.println(e);
        }
        checkInnerClass(srcFile,desFile);//检查并复制内部类
        System.out.println("srcFile = " + srcFile.getAbsolutePath());
    }

    //检查并复制内部类的编译文件
    public static void checkInnerClass(File srcFile,File desFile){
        File srcFilePath = new File(srcFile.getParent());
        String innerClassName = srcFile.getName().replace(".class","$");
        File flist[] = srcFilePath.listFiles();
        System.out.println("srcFilePath = " + srcFilePath.getAbsolutePath());
        System.out.println("flist = " + flist);
        for (File f : flist) {
            if(f.getName().contains(innerClassName)) {
                desFile = new File(desFile.getAbsolutePath().replace(desFile.getName(), f.getName()));
            try {
                copyFile(f, desFile);
                System.out.println(desFile.getName()+"复制完成");
            } catch (IOException e) {
                System.out.println(desFile.getName()+"复制失败");
                System.out.println(e);
            }
            }
        }
    }

    //复制文件
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            outBuff.flush();
        } finally {
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }

    //删除文件夹
    public static void deleteAllFilesOfDir(File path) {
        if (!path.exists())
            return;
        if (path.isFile()) {
            path.delete();
            return;
        }
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            deleteAllFilesOfDir(files[i]);
        }
        String parent = path.getParent();
        File parentPath = new File(parent);
        path.delete();
    }

    //当文件夹为空的时候删除这个文件夹的父文件夹
    public static void deleteParent(File path){
        String parent1 = path.getParent();
        File parentPath1 = new File(parent1);
        if(path.listFiles().length==0){
            System.out.println("parentPath = " + path.getAbsolutePath());
            path.delete();
            deleteParent(parentPath1);
        }
    }
}
