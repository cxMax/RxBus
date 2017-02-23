package lib.rxbus.lint;

import com.android.tools.lint.detector.api.Detector;

import java.util.List;

import lombok.ast.Node;

public class CheckParamDetector extends Detector implements Detector.JavaScanner{

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return super.getApplicableNodeTypes();
        //// TODO: 2017/2/23 there need more information to practice
    }
}
