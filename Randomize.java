public class Randomize {

  
  static int[] getRandomInts(int count){
    int[] toReturn = new int[count];
    for(var i = 0; i < count; i++){
      toReturn[i] = Integer.hashCode(i);
    }
    return toReturn;
  }

  public static int getRandomInt(int size) {
    return ("" + size).hashCode();
  }
  
}
