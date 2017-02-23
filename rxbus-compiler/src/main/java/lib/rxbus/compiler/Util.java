package lib.rxbus.compiler;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by cxmax on 2017/2/23.
 */

public class Util {
    public static Types TypeUtils;//处理TypeMirror
    public static Elements ElementUtils;//处理Element
    public static javax.annotation.processing.Filer Filer;//一般用于生成文件、获取文件
    public static javax.annotation.processing.Messager Messager;//打印信息用
    
}
