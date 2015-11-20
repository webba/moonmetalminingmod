package com.webbrar.wurm.mods;

import com.wurmonline.server.Server;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.behaviours.Action;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.*;
import javassist.bytecode.*;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
/**
 *
 * @author Webba
 */
public class MoonMetalMiningMod implements WurmMod, Configurable, PreInitable {
    private boolean useMoonMetalMiningMod = false;
    private boolean removeVeinCap = false;
    private boolean randomMoonMetalDrops = false;
    private static boolean staticRandomMoonMetalDrops = false;
    private static int staticRandomDropChance = 1000;
    private String actionMethodDesc = "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIZIISF)Z";
    private String createGemMethodDesc = "(IIIILcom/wurmonline/server/creatures/Creature;DZLcom/wurmonline/server/behaviours/Action;)Lcom/wurmonline/server/items/Item;";
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    @Override
    public void configure(Properties properties) {
        useMoonMetalMiningMod = Boolean.valueOf(properties.getProperty("useMoonMetalMiningMod", Boolean.toString(useMoonMetalMiningMod)));
        removeVeinCap = Boolean.valueOf(properties.getProperty("removeVeinCap", Boolean.toString(removeVeinCap)));
        randomMoonMetalDrops = Boolean.valueOf(properties.getProperty("randomMoonMetalDrops", Boolean.toString(randomMoonMetalDrops)));
        MoonMetalMiningMod.staticRandomMoonMetalDrops = randomMoonMetalDrops;
        MoonMetalMiningMod.staticRandomDropChance = Integer.valueOf(properties.getProperty("randomDropChance", Integer.toString(MoonMetalMiningMod.staticRandomDropChance)));
    }
    
    @Override
    public void preInit() {
        if(useMoonMetalMiningMod){
            if(removeVeinCap){
                removeMoonMetalVeinCap();
            }
            if(randomMoonMetalDrops){
                addRandomMoonMetalDrop();
            }
        }
        if(useMoonMetalMiningMod){
        }
    }
    
    private void addRandomMoonMetalDrop(){
        HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.TileRockBehaviour", "createGem", createGemMethodDesc, new InvocationHandlerFactory(){
            @Override 
            public InvocationHandler createInvocationHandler(){
                return new InvocationHandler(){
                    @Override
                    public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                        int tilex = (int)args[0];
                        int tiley = (int)args[1];
                        int createtilex = (int)args[2];
                        int createtiley = (int)args[3];
                        Creature performer = (Creature)args[4];
                        double power = (double)args[5];
                        boolean surfaced = (boolean)args[6];
                        Action act = (Action)args[7];
                        final byte rarity = (byte)((act != null) ? act.getRarity() : 0);
                        if(MoonMetalMiningMod.staticRandomMoonMetalDrops){
                            if (Server.rand.nextInt(MoonMetalMiningMod.staticRandomDropChance) == 0) {
                                final int templateID = ((Server.rand.nextInt(2)==0) ? 693: 697);
                                if (tilex < 0 && tiley < 0) {
                                    final Item metal = ItemFactory.createItem(templateID, (float)power, (String)null);
                                    metal.setLastOwnerId(performer.getWurmId());
                                    return metal;
                                }
                                final Item metal = ItemFactory.createItem(templateID, (float)power, (float)(createtilex * 4 + Server.rand.nextInt(4)), (float)(createtiley * 4 + Server.rand.nextInt(4)), Server.rand.nextFloat() * 360.0f, surfaced, rarity, -10L, (String)null);
                                metal.setLastOwnerId(performer.getWurmId());
                                performer.getCommunicator().sendNormalServerMessage("You find a chunk of a mysterious metal.");
                            }
                        }
                        return method.invoke(object, args);
                    }
                };
            }
        });
    }
    
    private void removeMoonMetalVeinCap(){
        try{
            logger.log(Level.INFO, "Removing Moon Metal vein ammount cap");
            ClassPool cp = HookManager.getInstance().getClassPool();
            CtClass caveWallClass = cp.get("com.wurmonline.server.behaviours.CaveWallBehaviour");
            
            MethodInfo mi = caveWallClass.getMethod("action", actionMethodDesc).getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            ConstPool constPool= ca.getConstPool();

            CodeIterator codeIterator = ca.iterator();
            while(codeIterator.hasNext()) {

                int pos = codeIterator.next();
                int op = codeIterator.byteAt(pos);
                int siPusharg = codeIterator.u16bitAt(pos+1);
                
                //sipush(3) if_icmpeq(3) iload(2) sipush(3) if_icmpne(3) TOOVERWRITE(bipush(2) to iload(2)) TOGET(istore(2)) overwritepos: 14-15 getpos = 17
                int resourceRef = codeIterator.byteAt(pos+17);
                
                
                if (op == CodeIterator.SIPUSH && siPusharg == 693){
                    logger.log(Level.INFO, "Found bytecode pattern for moon metal veins");
                    codeIterator.writeByte(CodeIterator.ILOAD, pos+14);
                    codeIterator.writeByte(resourceRef, pos+15);
                    logger.log(Level.INFO, "Moon metal vein cap removed");
                    break;
                }
            }
            mi.rebuildStackMap(cp);
        }
        catch(NotFoundException e)
        {
            throw new HookException(e);
        }
        catch(BadBytecode e){
            e.printStackTrace();
        }
    }
}