public class DynamicArray<T> implements List<T>, Comparable<DynamicArray<T>> {
    private Object[] data;

    public DynamicArray(int size){
        data = new Object[size];
    }

    public  void set(int i, T element){
        data[i] = element;
    }
    public  T get(int i){
        return (T)data[i];
    }

    public int compareTo(DynamicArray<T> other){
        return 0;
    }

    public static void main(String[] args){
        DynamicArray<Integer> a = new DynamicArray<Integer>(10);
        //DynamicArray a = new DynamicArray(10);
        a.set(2, 3);
        int i = a.get(2);
    }
}


interface List<T>{
    public void set(int i, T element);
    public T get(int i);
}

interface Comparable<T>{
    public int compareTo(T other);
}
