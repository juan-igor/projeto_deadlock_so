/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projeto_so;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

/**
 *
 * @author Juan Igor
 */
public class Processo extends Thread{
    
    private int ID, Ts, Tu, time = 0;
    private int[] resourcesVector = new int[Principal.resources_qtt];
    private int[] requestVector = new int[Principal.resources_qtt];
    private static boolean alive = true;
    private ArrayList<Semaphore> resources;
    private ArrayList<String> utilizingResourcesNames = new ArrayList<>();
    private ArrayList<Integer> utilizingResourcesIDs = new ArrayList<>();
    private ArrayList<Integer> resourceCollectedTime = new ArrayList<>();
    private final Random IDgenerator = new Random();
    private String uResources = "", sResources = "";
    
    public Processo(int ID, int Ts, int Tu){
        this.ID = ID;
        this.Ts = Ts;
        this.Tu = Tu;
        this.setPriority(2);
        
        for(int i=0; i<Principal.resources_qtt; i++){
            resourcesVector[i] = 0;
            requestVector[i] = 0;
        }
    }
    
    @Override
    public void run(){
        while(alive){
            if(liberar()) libera_recurso();
            
            if(solicitar()) solicita_recurso();
            
            try {
                sleep(1000);
            } catch (InterruptedException ex) {}
            finally{
                time++;
            }
        }
    }
    
    private void solicita_recurso(){
        int max_rID = Principal.resources_qtt;
        int random_rID = (int) (IDgenerator.nextInt(max_rID));
        
        if(!(resourcesVector[random_rID] == Principal.resourcesInstancies[random_rID])){
            try{
                Principal.MUTEX.acquire();
                sResources = Principal.resourceNames.get(random_rID);
                requestVector[random_rID]++;
                Principal.processesRequests.set(ID-1, requestVector);
                setTable("Solicitando");
                Principal.logAdd(ID, "Solicitando: "+Principal.resourceNames.get(random_rID));
            } catch(InterruptedException e){}
            Principal.MUTEX.release();

            try {
                if(Principal.resourceSemaphores.get(random_rID).availablePermits() == 0){
                    setTable("Aguardando Recurso");
                }
                Principal.resourceSemaphores.get(random_rID).acquireUninterruptibly();
                Principal.MUTEX.acquire();
                requestVector[random_rID]--;
                Principal.processesRequests.set(ID-1, requestVector);
                sResources = "Nenhum";
                if(!utilizingResourcesNames.contains(Principal.resourceNames.get(random_rID))){
                    utilizingResourcesNames.add(Principal.resourceNames.get(random_rID));
                }
                utilizingResourcesIDs.add(random_rID);
                resourcesVector[random_rID]++;
                Principal.processesUtilizing.set(ID-1, resourcesVector);
                resourceCollectedTime.add(time);
            } catch (InterruptedException ex) {}
            setTable("Executando");
            Principal.MUTEX.release();
        }
    }
    
    private void libera_recurso(){
        int rID = utilizingResourcesIDs.get(0);
        
        try{
            Principal.MUTEX.acquire();
            setTable("Liberando");
            Principal.logAdd(ID, "Liberando: "+Principal.resourceNames.get(rID));
            Principal.resourceSemaphores.get(rID).release();
            utilizingResourcesIDs.remove(0);
            resourcesVector[rID]--;
            Principal.processesUtilizing.set(ID-1, resourcesVector);
            if(!utilizingResourcesIDs.contains(rID)){
               utilizingResourcesNames.remove(Principal.resourceNames.get(rID));
            }
            setTable("Executando");
            Principal.MUTEX.release();
        }catch(InterruptedException ex){}
        
    }
    
    private void setTable(String state){
        uResources = utilizingResources();
        Principal.setProcessInTable(ID, state, uResources, sResources);
    }
    
    private String utilizingResources(){
        if(utilizingResourcesNames.isEmpty()) return "Nenhum";
        else if(utilizingResourcesNames.size() == 1) return utilizingResourcesNames.get(0);
        else{
            String response = "";
            int i;
            for(i=0; i<(utilizingResourcesNames.size()-1); i++){
                response += utilizingResourcesNames.get(i)+", ";
            }
            response += utilizingResourcesNames.get(i);
            return response;
        }
    }
    
    private boolean liberar(){
        if(!resourceCollectedTime.isEmpty()){
           if(Tu <= (time-resourceCollectedTime.get(0))){
               resourceCollectedTime.remove(0);
               return true;
           } 
        }
        return false;
    }
    
    private boolean solicitar(){
        return (time%Ts == 0);
    }
    
    public static boolean excluir_processo(){
        alive = false;
        
        return true;
    }
    
    public int get_id(){
        return ID;
    }
}
