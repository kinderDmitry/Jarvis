package com.jarvis.homemultitool.agent;

import android.content.Context;
import java.util.*;

/** Extensible registry; tools are metadata here, while existing production handlers perform actions. */
public final class JarvisToolRegistry {
    private final LinkedHashMap<String,JarvisTool> tools=new LinkedHashMap<>();
    public void register(JarvisTool tool){if(tool!=null&&!tools.containsKey(tool.name()))tools.put(tool.name(),tool);}
    public JarvisTool get(String name){return tools.get(name);}
    public List<JarvisTool> all(){return new ArrayList<>(tools.values());}
    public List<JarvisTool> available(Context c){List<JarvisTool> out=new ArrayList<>();for(JarvisTool t:tools.values())if(t.isAvailable(c))out.add(t);return out;}
}
