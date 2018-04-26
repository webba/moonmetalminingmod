package com.webbrar.wurm.mods.moonmetalmining;

import com.wurmonline.server.Server;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.behaviours.Action;

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
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
/**
 *
 * @author Webba
 */
public class MoonMetalMiningMod implements WurmServerMod, Configurable, PreInitable {
    private boolean useMoonMetalMiningMod = false;
    private boolean changeVeinCap = false;
    private boolean changeHomeVeinCap = false;
    private int newVeinCap = 10000;
    private int newHomeVeinCap = 100;
    private boolean randomMoonMetalDrops = false;
    private static int staticRandomGlimmersteelChance = 3000;
    private static int staticRandomAdamantiteChance = 3000;
    private static int staticRandomSeryllChance = 3000;
    private String actionMethodDesc = "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIZIIISF)Z";
    private String createGemMethodDesc = "(IIIILcom/wurmonline/server/creatures/Creature;DZLcom/wurmonline/server/behaviours/Action;)Lcom/wurmonline/server/items/Item;";
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    @Override
    public void configure(Properties properties) {
        useMoonMetalMiningMod = Boolean.valueOf(properties.getProperty("useMoonMetalMiningMod", Boolean.toString(useMoonMetalMiningMod)));
        changeVeinCap = Boolean.valueOf(properties.getProperty("changeVeinCap", Boolean.toString(changeVeinCap)));
        changeHomeVeinCap = Boolean.valueOf(properties.getProperty("changeHomeVeinCap", Boolean.toString(changeHomeVeinCap)));
        newVeinCap = Integer.valueOf(properties.getProperty("newVeinCap", Integer.toString(newVeinCap)));
        newVeinCap = Math.max(1, newVeinCap);
        newHomeVeinCap = Integer.valueOf(properties.getProperty("newHomeVeinCap", Integer.toString(newHomeVeinCap)));
        newHomeVeinCap = Math.max(1, Math.min(100, newHomeVeinCap));
        randomMoonMetalDrops = Boolean.valueOf(properties.getProperty("randomMoonMetalDrops", Boolean.toString(randomMoonMetalDrops)));
        MoonMetalMiningMod.staticRandomGlimmersteelChance = Integer.valueOf(properties.getProperty("randomGlimmersteelDropChance", Integer.toString(MoonMetalMiningMod.staticRandomGlimmersteelChance)));
        MoonMetalMiningMod.staticRandomAdamantiteChance = Integer.valueOf(properties.getProperty("randomAdamantiteDropChance", Integer.toString(MoonMetalMiningMod.staticRandomGlimmersteelChance)));
        MoonMetalMiningMod.staticRandomSeryllChance = Integer.valueOf(properties.getProperty("randomSeryllDropChance", Integer.toString(MoonMetalMiningMod.staticRandomSeryllChance)));
    }
    
    @Override
    public void preInit() {
        if(useMoonMetalMiningMod){
            if(changeVeinCap){
                removeMoonMetalVeinCap();
                logger.log(Level.INFO, "MoonMetalMining: Moon metal vein cap changed");
            }
            if(randomMoonMetalDrops){
                addRandomMoonMetalDrop();
                logger.log(Level.INFO, "MoonMetalMining: Random moon metal drops added.");
            }
            if(changeHomeVeinCap){
                changeHomeServerVeinCap();
                logger.log(Level.INFO, "MoonMetalMining: Home server vein cap changed.");
            }
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
                        int tempid = 0;
                        if(MoonMetalMiningMod.staticRandomAdamantiteChance > 0){
                            if (Server.rand.nextInt(MoonMetalMiningMod.staticRandomAdamantiteChance) == 0) {
                                tempid = 693;
                            }
                        }
                        if(MoonMetalMiningMod.staticRandomGlimmersteelChance > 0){
                            if (tempid == 0 && Server.rand.nextInt(MoonMetalMiningMod.staticRandomGlimmersteelChance) == 0) {
                                tempid = 697;
                            }
                        }
                        if(MoonMetalMiningMod.staticRandomSeryllChance > 0){
                            if (tempid == 0 && Server.rand.nextInt(MoonMetalMiningMod.staticRandomSeryllChance) == 0) {
                                tempid = 837;
                            }
                        }
                        if (tempid != 0){
                            if (tilex < 0 && tiley < 0) {
                                final Item metal = ItemFactory.createItem(tempid, (float)power, (String)null);
                                metal.setLastOwnerId(performer.getWurmId());
                                return metal;
                            }
                            final Item metal = ItemFactory.createItem(tempid, (float)power, (float)(createtilex * 4 + Server.rand.nextInt(4)), (float)(createtiley * 4 + Server.rand.nextInt(4)), Server.rand.nextFloat() * 360.0f, surfaced, rarity, -10L, (String)null);
                            metal.setLastOwnerId(performer.getWurmId());
                            performer.getCommunicator().sendNormalServerMessage("You find a chunk of a mysterious metal.");
                        }
                        return method.invoke(object, args);
                    }
                };
            }
        });
    }
    
    private void removeMoonMetalVeinCap(){
        try{
            logger.log(Level.INFO, "Changing Moon Metal vein ammount cap");
            ClassPool cp = HookManager.getInstance().getClassPool();
            CtClass caveWallClass = cp.get("com.wurmonline.server.behaviours.CaveWallBehaviour");
            
            MethodInfo mi = caveWallClass.getMethod("action", actionMethodDesc).getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            ConstPool constPool= ca.getConstPool();
            int capRef = constPool.addIntegerInfo(newVeinCap);

            CodeIterator codeIterator = ca.iterator();
            
            while(codeIterator.hasNext()) {

                int pos = codeIterator.next();
                int op = codeIterator.byteAt(pos);
                if (op == CodeIterator.SIPUSH)
                {
                	int siPusharg = codeIterator.u16bitAt(pos+1);
                	if (siPusharg == 693)
                	{
                		logger.log(Level.INFO, "Found bytecode pattern for moon metal veins");
                        codeIterator.insertGap(pos+14, 1);
                        codeIterator.writeByte(CodeIterator.LDC_W, pos+14);
                        codeIterator.write16bit(capRef, pos+15);
                        codeIterator.insertGap(pos-7, 1);
                        codeIterator.writeByte(CodeIterator.LDC_W, pos-7);
                        codeIterator.write16bit(capRef, pos-6);
                        logger.log(Level.INFO, "Moon metal vein cap changed");
                        break;
                	}
                }
            }
            
            mi.rebuildStackMap(cp);
        }
        catch(NotFoundException e)
        {
            throw new HookException(e);
        }
        catch(BadBytecode e){
            System.out.println("BAD BYTECODE ERROR ----- ");
            //e.printStackTrace();
        }
    }
    private void changeHomeServerVeinCap(){
        try{
            this.logger.log(Level.INFO, "Changing home server vein quality cap");
            ClassPool cp = HookManager.getInstance().getClassPool();
            CtClass tileRockClass = cp.get("com.wurmonline.server.behaviours.TileRockBehaviour");
            
            MethodInfo mi = tileRockClass.getConstructor(Descriptor.ofConstructor(new CtClass[]{})).getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            ConstPool cpool = ca.getConstPool();
            

            CodeIterator codeIterator = ca.iterator();
            while(codeIterator.hasNext()) {

                int pos = codeIterator.next();
                int op = codeIterator.byteAt(pos);
                
                if (op == CodeIterator.BIPUSH){
                	int biPusharg = codeIterator.byteAt(pos+1);
                    if(biPusharg == 50 && codeIterator.getCodeLength() > pos + 2){
                        int nextop = codeIterator.byteAt(pos+2);
                    	if(nextop == CodeIterator.PUTSTATIC){
                            int nextarg = codeIterator.u16bitAt(pos+3);
                    		if(cpool.getFieldrefName(nextarg).equals("MAX_QL")){
			                    logger.log(Level.INFO, "Found bytecode pattern for home server metal veins");
			                    codeIterator.writeByte((byte)newHomeVeinCap, pos+1);
			                    logger.log(Level.INFO, "Home server vein cap changed");
                    		}
                    	}
	                    break;
                    }
                }
            }
            mi.rebuildStackMap(cp);
        }
        catch(NotFoundException e)
        {
            throw new HookException(e);
        }
        catch(BadBytecode e){
            System.out.println("BAD BYTECODE ERROR ----- ");
            e.printStackTrace();
        }
    }
}