package org.bhavaya.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 23/06/14
 * Time: 13:53
 */
public class CircularBuffer<T> {

    protected List<T> buffer;
    protected int currentPtr;
    private int maxSize;

    public CircularBuffer(int maxSize){
        this.maxSize = maxSize;
        buffer = new ArrayList<T>();
        currentPtr = -1;
    }

    public void add(T t){
        buffer.add(t);
        List<T> tempList = new ArrayList<T>();
        for (int i=buffer.size()-1; i>=0; i--){
            T tb = buffer.get(i);
            if(!tempList.contains(tb)){
                tempList.add(tb);
            }
        }
        if(buffer.size() > maxSize){
           buffer.remove(0);
        }
        buffer.clear();
        for(int i=tempList.size()-1; i>=0 && buffer.size() < maxSize; i--){
          buffer.add(tempList.get(i));
        }
        currentPtr = buffer.size()-1;
    }

    public void remove(T t){

        int ptr = -1;
        Iterator iter = buffer.iterator();
        System.out.println("currentPtr => " + currentPtr);
        while(iter.hasNext()){
            Object obj = iter.next();
            ptr+=1;
            if(obj.equals(t)){
                iter.remove();
                if(currentPtr >= ptr && currentPtr >0 ){
                    currentPtr -=1;
                }
            }
        }
    }


    public boolean canIncCurrentPtr(){
        return currentPtr < buffer.size()-1;
    }
     public boolean canDecCurrentPtr(){
        return currentPtr > 0;
    }

    public T getCurrent(){
        return buffer.get(currentPtr);
    }

    public T getNext(){
        if(canIncCurrentPtr()){
            currentPtr += 1;
        }
        return buffer.get(currentPtr);
    }
    public T getPrev(){
        if(canDecCurrentPtr()){
            currentPtr -= 1;
        }
        if(currentPtr <0){
            return null;
        }
        return buffer.get(currentPtr);
    }

}
