package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Bot {
    private GameState gameState;
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        //Atribut dari bot
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get(); // Player A
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get(); // Player B
    }
    
    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        // Early game (Build Energy Building selama belum di attack)  
        String command = buildEnergyIfNoEnemyAttack();
        // Jika ada attack musuh (command = doNothing) jalankan algoritma greedy berdasarkan opponent building health 
        if (command.equals(doNothing()))
            command = attackMostLessHealth();
        return command;
    }

    /**
     * Jika dalam satu baris tidak ada attack building musuh dan belum ada energy building, maka kita akan membangun energy building
     * @return command
     **/
    private String buildEnergyIfNoEnemyAttack() {
        String command = doNothing(); // Inisiasi command 
        List<Integer> safePlaceList = getSafePlace(); //Mendapatkan list tempat yang tidak di attack musuh
        Collections.shuffle(safePlaceList); //random list agar build energy lebih acak
        for (int safePlace : safePlaceList) {
            int myEnergyOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ENERGY, safePlace).size();
            if (myEnergyOnRow == 0 && myself.energy < getPriceForBuilding(BuildingType.ATTACK)) { //Jika tidak ada energy building pada baris dan energy tidak cukup untuk membangun attack
                if (canAffordBuilding(BuildingType.ENERGY))
                    command = placeBuildingInRowFromBack(BuildingType.ENERGY, safePlace); //Membangun energy building pada tempat yang aman
                break;
            } 
        }
        return command;
    }
    
    /**
     * Menyerang baris musuh yang memiliki health paling sedikit
     * @return command
     **/
    private String attackMostLessHealth() {
        String command = doNothing(); // Inisiasi command
        List<Integer> healthList = getHealthBuildingInRow(opponent.playerType); //Mendapatkan list health tiap baris musuh
        int minIndex = healthList.indexOf(Collections.min(healthList)); //Menghasilkan minIndex yang merupakan index dimana terdapat baris dengan health musuh paling sedikit
        List<Integer> minIndexList = getListOfMinHealthIndex(healthList); //Mendapatkan list index dimana terdapat baris dengan health musuh paling sedikit
        if (minIndexList.size() > 1) { //Jika list Index lebih besar dari 1, update minIndex
            List<Integer> attackOnRowList = new ArrayList<>(); 
            for (int i = 0; i < minIndexList.size(); i++) {
                int myAttackOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ATTACK, minIndexList.get(i)).size();
                attackOnRowList.add(myAttackOnRow);
            }
            int minattack = Collections.min(attackOnRowList);
            for (int i = 0;i< minIndexList.size();i++) {
                int myAttackOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ATTACK, minIndexList.get(i)).size();
                if (myAttackOnRow==minattack) {
                    minIndex = minIndexList.get(i);
                    break;
                }
            }
        }
        if (canAffordBuilding(BuildingType.ATTACK)) { //Jika dapat membangun attack building
            command = placeBuildingInRowFromFront(BuildingType.ATTACK, minIndex); //Membangun building pada minIndex dikolom paling depan
        }
        return command;
    }
    
    /**
     * Membangun building di kolom paling belakang secara random
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromBack(BuildingType buildingType) {
        for (int i = 0; i < gameWidth / 2; i++) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) { //Jika tidak kosong
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Membangun building di kolom paling depan secara random
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromFront(BuildingType buildingType) {
        for (int i = (gameWidth / 2) - 1; i >= 0; i--) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) { //Jika tidak kosong
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Membangun building di kolom dengan prioritas dari depan
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Membangun building di kolom dengan prioritas dari belakang
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Construct build command
     *
     * @param x            the x
     * @param y            the y
     * @param buildingType the building type
     * @return the result
     **/
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%s,%s,%s", String.valueOf(x), String.valueOf(y), buildingType.getType());
    }

    /**
     * Mengambil semua building berdasarkan filter pada kolom y
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllBuildingsInRowForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Mengambil semua sel yang kosong pada kolom x
     *
     * @param x the x
     * @return the result
     **/
    private List<CellStateContainer> getListOfEmptyCellsForColumn(int x) {
        return gameState.getGameMap().stream()
                .filter(c -> c.x == x && isCellEmpty(x, c.y))
                .collect(Collectors.toList());
    }

    /**
     * Mengecek apakah sel(x,y) kosong
     *
     * @param x the x
     * @param y the y
     * @return the result
     **/
    private boolean isCellEmpty(int x, int y) {
        Optional<CellStateContainer> cellOptional = gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .findFirst();

        if (cellOptional.isPresent()) {
            CellStateContainer cell = cellOptional.get();
            return cell.getBuildings().size() <= 0;
        } else {
            System.out.println("Invalid cell selected");
        }
        return true;
    }

    /**
     * Mengecek apkah building type dapat dibangun
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= getPriceForBuilding(buildingType);
    }

    /**
     * Mengambil biaya untuk membangun building
     *
     * @param buildingType the building type
     * @return the result
     **/
    private int getPriceForBuilding(BuildingType buildingType) {
        return gameDetails.buildingsStats.get(buildingType).price;
    }

    /**
     * Do nothing command
     *
     * @return the result
     **/
    private String doNothing() {
        return "";
    }
    
    /**
     * Mengambil list tempat yang aman(tidak ada attack building musuh)
     *
     * @return the result
     **/
    private List<Integer> getSafePlace() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < gameHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsInRowForPlayer(opponent.playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
            if (enemyAttackOnRow == 0) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Mengambil list total health building dalam satu baris
     *
     * @return the result
     **/
    private List<Integer> getHealthBuildingInRow(PlayerType playerType) {
        List<Integer> healthList = new ArrayList<>();
        for (int i = 0; i < gameHeight; i++) {
            int rowHealth = 0;
            int enemyAttackOnRow = getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();//Banyaknya attack building pada satu baris
            int enemyEnergyOnRow = getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ENERGY, i).size();//Banyaknya energy building pada satu baris
            for (Building attackBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ATTACK, i)) { //iterasi tiap attack building pada 1 row
                if (enemyAttackOnRow > 1) { //Jika banyaknya attack building dalam satu row lebih besar dari satu
                    rowHealth += attackBuilding.health; //health pada 1 row ditambah attack building health (prioritas pada row berkurang)
                } else {
                    rowHealth -= attackBuilding.health; //health pada 1 row dikurangi attack building health (prioritas pada row bertambah)
                }
            }
            for (Building defenceBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.DEFENSE, i)) { //iterasi tiap defense building pada 1 row
                rowHealth += defenceBuilding.health*100; //health pada 1 row ditambah defence building health dikali 100 (row ditempatkan pada prioritas terakhir)
            }
            for (Building energyBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ENERGY, i)) { //iterasi tiap energy building pada 1 row
                if (enemyEnergyOnRow == 1) { 
                    rowHealth -= energyBuilding.health*100; // Jika enemy energy pada row hanya 1, health pada 1 row dikurangi energy building health (prioritas pada row diutamakan) 
                } // Jika enemy energy pada row > 1, health pada 1 row tersebut tidak ditambah health tiap energy building health (prioritas row tidak dikurang/ditambah)
            }
            healthList.add(i, rowHealth); //masukkan health pada 1 row pada list healthList
        }
        return healthList;
    }
    
    /**
     * Mengambil list yang berisi index tempat minimum health pada list healthList (index minimum health bisa lebih dari satu)
     *
     * @return the result
     **/
    private List<Integer> getListOfMinHealthIndex(List<Integer> healthList) {
        List<Integer> minHealthIndexList = new ArrayList<>();
        int minHealth = Collections.min(healthList);
        for (int i = 0; i < healthList.size(); i++) {
            if (healthList.get(i) == minHealth) {
                minHealthIndexList.add(i); // menambahkan index tempat minHealth berada pada list minHealthIndexList
            }
        }
        return minHealthIndexList;
    }
}