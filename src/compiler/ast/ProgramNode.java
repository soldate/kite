package compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramNode extends Node {
    public final List<ClassDefNode> classes = new ArrayList<>();
    public final Map<String, ClassDefNode> types = new HashMap<>();
    public FuncDefNode globalMain; // pode ser null

    public void add(ClassDefNode classDef) {
        if (classDef != null) {
            types.put(classDef.name, classDef);
            classes.add(classDef);
        }
    }

    @Override
    public String toString() {
        return "ProgramNode";
    }
}
