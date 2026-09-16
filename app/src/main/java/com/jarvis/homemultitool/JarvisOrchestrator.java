package com.jarvis.homemultitool;

import java.util.*;

/** Agent execution boundary: plan -> execute existing real handlers -> record outcome. */
public final class JarvisOrchestrator {
    private final JarvisTaskPlanner planner=new JarvisTaskPlanner();
    private final JarvisActionCenter actions;
    private boolean executingPlan;
    public JarvisOrchestrator(android.content.Context c){actions=new JarvisActionCenter(c);}
    public JarvisActionCenter actionCenter(){return actions;}
    public boolean tryHandle(String raw, JarvisEngine engine){
        if(executingPlan)return false;
        JarvisTaskPlanner.Plan p=planner.plan(raw);
        if(!p.isCompound())return false;
        executingPlan=true;
        final String id=actions.start(p.original,p.steps);
        engine.externalState("ПЛАНИРОВАНИЕ");
        engine.externalReply("Разбиваю задачу на "+p.steps.size()+" действия.");
        try{
            for(int i=0;i<p.steps.size();i++){
                String step=p.steps.get(i); actions.progress(id,i); engine.externalState("ВЫПОЛНЯЮ "+(i+1)+"/"+p.steps.size());
                engine.handlePlannedStep(p.steps.get(i));
            }
            actions.complete(id); engine.externalState("ГОТОВ");
            engine.externalReply("Составная задача завершена.");
        }catch(Throwable t){actions.fail(id);engine.externalState("ОШИБКА");engine.externalReply("Выполнение остановлено: одно из действий завершилось ошибкой.");}
        finally{executingPlan=false;}
        return true;
    }
}
