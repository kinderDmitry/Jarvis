package com.jarvis.homemultitool;

import java.util.*;
import java.util.regex.*;

/**
 * Converts a natural-language multi-action request into ordered executable steps.
 * It deliberately uses conservative boundaries: it will not split arbitrary prose.
 */
public final class JarvisTaskPlanner {
    public static final class Plan {
        public final String original; public final List<String> steps;
        Plan(String o,List<String> s){original=o;steps=s;}
        public boolean isCompound(){return steps.size()>1;}
    }
    public Plan plan(String input){
        String raw=input==null?"":input.trim();
        if(raw.isEmpty())return new Plan(raw,Collections.<String>emptyList());
        String normalized=JarvisSmartRouter.normalize(raw);
        String[] parts=normalized.split("\\s+(?:и затем|затем|после этого|а потом|потом)\\s+|\\s*;\\s*",-1);
        List<String> steps=new ArrayList<>();
        for(String p:parts){String s=p.trim();if(!s.isEmpty())steps.add(s);}
        if(steps.size()==1){
            // Only split the high-confidence action connector " и ". Avoid splitting common noun phrases.
            Matcher m=Pattern.compile("^(.+?)\\s+и\\s+(открой|запусти|включи|поставь|поставить|выключи|выключить|позвони|напиши|найди|покажи|переведи|сохрани|запомни|установи|поставить на паузу|сделай)\\b(.+)$",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(normalized);
            if(m.find()){String a=m.group(1).trim(),b=(m.group(2)+m.group(3)).trim();if(a.length()>2&&b.length()>2){steps.clear();steps.add(a);steps.add(b);}}
        }
        return new Plan(raw,steps);
    }
}
