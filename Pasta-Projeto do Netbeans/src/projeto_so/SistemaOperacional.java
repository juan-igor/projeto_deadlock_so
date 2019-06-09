/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projeto_so;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juan
 */
public class SistemaOperacional extends Thread{

    private long interval;
    private boolean alive = true;
    private int[] freeResourcesQtt;
    private ArrayList<Boolean> DLprocesses;
    private boolean msgTypeNDL = false;
    
    public SistemaOperacional(){
        this.interval = Principal.SOinterval;
        this.setPriority(2);
    }
    
    @Override
    public void run(){
        while(alive){
            checar_sistema();
            try {
                sleep(interval);
            } catch (InterruptedException ex) {}
        }
    }
    
    public void checar_sistema(){
        if(!Principal.processes.isEmpty()){
            try { Principal.MUTEX.acquire(); } catch (InterruptedException ex) {}
            DLprocesses = getFalseArrayList(Principal.processes.size());
            freeResourcesQtt = Principal.getFreeResources();
            int[][] requestMatrix = getRequestMatrix();
            int[][] alocationMatrix = getAlocationMatrix();
            int markedProcesses = 0, flag;
            
            for(int i=0; i<DLprocesses.size(); i++){
                if(Principal.processes.get(i) != null){
                    flag = 0;
                    for(int j=0; j<freeResourcesQtt.length; j++){
                        if(requestMatrix[i][j] > freeResourcesQtt[j]){
                            flag++;
                        }
                    }
                    if(flag == 0){
                        for(int j=0; j<freeResourcesQtt.length; j++){
                           freeResourcesQtt[j] += alocationMatrix[i][j]; 
                        }
                    }
                    else{
                        DLprocesses.set(i, true);
                        markedProcesses++;
                    }
                }
            }
            
            boolean modify = false;
            
            do{
                modify = false;
                for(int i=0; i<DLprocesses.size(); i++){
                    if(Principal.processes.get(i) != null && DLprocesses.get(i)){
                        flag = 0;
                        for(int j=0; j<freeResourcesQtt.length; j++){
                            if(requestMatrix[i][j] > freeResourcesQtt[j]){
                                flag++;
                            }
                        }
                        if(flag == 0){
                            for(int j=0; j<freeResourcesQtt.length; j++){
                               freeResourcesQtt[j] += alocationMatrix[i][j]; 
                            }
                            modify = true;
                            DLprocesses.set(i, false);
                            markedProcesses--;
                        }
                    }
                }
            }while(DLprocesses.contains(true) && modify);
            
            if(markedProcesses > 1) showDeadLocks();
            else showNoDeadLock();
            
            Principal.MUTEX.release();
        } 
    }
    
    private void showDeadLocks(){
        String message = "";
        ArrayList<Integer> deadLockProcesses = new ArrayList<>();
        int i;

        for(i=0; i<DLprocesses.size(); i++){
            if(DLprocesses.get(i)){
                deadLockProcesses.add(i+1);
            }
        }

        for(i=0; i<deadLockProcesses.size()-1; i++){
            message += deadLockProcesses.get(i)+", ";
        }
        message += deadLockProcesses.get(i);

        Principal.DL_logAdd(message);
        
        msgTypeNDL = false;
    }
    
    private void showNoDeadLock(){
        if(!msgTypeNDL){
            Principal.NoDL_logAdd();
            msgTypeNDL = true;
        }
    }
    
    private int[][] getRequestMatrix(){
        int[][] matrix = new int [Principal.processes.size()][Principal.resources_qtt];
        
        for(int i=0; i<Principal.processes.size(); i++){
            for(int j=0; j<Principal.resources_qtt; j++){
                if(Principal.processes.get(i)!= null){
                    matrix[i][j] = Principal.processesRequests.get(i)[j];
                } else{
                    matrix[i][j] = 0;
                }
            }
        }
        
        return matrix;
    }
    
    private int[][] getAlocationMatrix(){
        int[][] matrix = new int [Principal.processes.size()][Principal.resources_qtt];
        
        for(int i=0; i<Principal.processes.size(); i++){
            for(int j=0; j<Principal.resources_qtt; j++){
                if(Principal.processes.get(i)!= null){
                    matrix[i][j] = Principal.processesUtilizing.get(i)[j];
                } else{
                    matrix[i][j] = 0;
                }
            }
        }
        
        return matrix;
    }
    
    private ArrayList<Boolean> getFalseArrayList(int size){
        ArrayList<Boolean> falseArray = new ArrayList<>();
        
        for(int i=0; i<size; i++){
            falseArray.add(false);
        }
        
        return falseArray;
    }
    
    public void SO_NoCheck(){
        alive = false;
    }
}
