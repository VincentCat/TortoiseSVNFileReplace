import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SVNFileReplace {

    public static String exportPath = "";//tortoiseSVN������Ŀ¼,src����һ��
    public static String projectPath = "";//��Ŀ��Ŀ¼,WEB-INF����һ��
    public static String projectName = "";
    public static List<String> firstPrefixList = new ArrayList<String>();//����Դ�ļ����ļ��еĵ�һ��ǰ׺list
    public static List<String> prefixList = new ArrayList<String>();//�����ļ����д���Դ�ļ��ĵ�һ��ǰ׺list

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while(!checkPath("��Ŀ�ĸ�Ŀ¼��WEB-INF�ļ��е���һ��Ŀ¼��",projectPath,"WEB-INF")){
            projectPath = scanner.next();
        }
        while(!checkPath("TortoiseSVN��������Ŀ¼(����src�ϼ�����java�ļ��У����û��src�ļ��У����½�һ���յ�src�ļ���)",exportPath,"src")){
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

    //���·��
    private static boolean checkPath(String name,String path,String folder){
        Boolean flag = false;
        if("".equals(path)||path==null) {
            System.out.println("������" + name + ":");
            return flag;
        }
        if(!new File(path).exists()){
            System.out.println(name+"�����ڣ�����������:");
            return flag;
        }
        File flist[] = new File(path).listFiles();
        for (File f : flist) {
            if(f.getName().endsWith(folder))
                flag = true;
        }
        if(!flag){
            System.out.println(name+"����ȷ������������:");
        }
        return flag;
    }

    //�õ�����Դ·��
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

    //�õ�������Ϊ����Դ·����ͷ
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

    //�õ�����javaĿ¼�д���java�ļ���Ŀ¼ǰ׺
    private static void getPrefix(File file){
        File flist[] = file.listFiles();
        for (File f : flist) {
            if (f.isDirectory()) {
                for (int i = 0; i < firstPrefixList.size(); i++) {
                    if(f.getAbsolutePath().endsWith(firstPrefixList.get(i))){//���ڱ�������Ŀ¼��������������ͷǰ׺list���ļ��е�ʱ��
                        String prefix = f.getParent().replace(exportPath,"");//��Ŀ¼ǰ׺ȥ��
                        Boolean exist = false;
                        String prefixTemp = prefix;
                        while (prefixTemp.contains("\\")){
                            int last = prefixTemp.lastIndexOf("\\");
                            if(last>0) prefixTemp = prefixTemp.substring(0, last);
                            else break;
                            if (prefixList.contains(prefixTemp)) exist = true;//��ǰ׺list�д��ھͲ���
                        }
                        if(!prefixList.contains(prefix)&&!exist) {
                            prefixList.add(prefix);
                        }
                    }
                }
                getPrefix(f);//����
            }
        }
    }

    //�ҵ�java��
    private static void getFile(File file,String prefix) {
        File flist[] = file.listFiles();
        if (flist == null || flist.length == 0) {
            System.out.println("tortoiseSVN����Ŀ¼��û���ļ�");
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

    //�����ļ�·���������ļ���
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
            System.out.println(desFile.getName()+"�������");
        } catch (IOException e) {
            System.out.println(desFile.getName()+"����ʧ��");
            System.out.println(e);
        }
        checkInnerClass(srcFile,desFile);//��鲢�����ڲ���
        System.out.println("srcFile = " + srcFile.getAbsolutePath());
    }

    //��鲢�����ڲ���ı����ļ�
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
                System.out.println(desFile.getName()+"�������");
            } catch (IOException e) {
                System.out.println(desFile.getName()+"����ʧ��");
                System.out.println(e);
            }
            }
        }
    }

    //�����ļ�
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

    //ɾ���ļ���
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

    //���ļ���Ϊ�յ�ʱ��ɾ������ļ��еĸ��ļ���
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
