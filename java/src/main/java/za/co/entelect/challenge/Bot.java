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
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();
        // opponentAttack = getAllBuildings(opponent.playerType, buildingType)
    }
    
    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = greedyByAttack();
        return command;
    }

    public String greedyByAttack() {
        // defendRow first 
        String command = doNothing();
        // String command = defendRowIfEnemyAttack(doNothing());

        // if (command.equals(doNothing())) 
        //     command = buildAnotherDefenceBuilding(command);
        
        if (command.equals(doNothing())) 
            command = buildEnergyIfNoEnemyAttack(command);

        if (command.equals(doNothing())) 
            command = buildAttack(command);

        // if (command.equals(doNothing())) 
        //     command = buildAttackBehindDefenceBuilding(command); 
    
        // if (command.equals(doNothing())) {
        //     command = attackMostLessHealth(command);
        // }

        return command;
    }

    /**
     * If the enemy has an attack building and I don't have a blocking wall, then block from the front.
     * @return command
     **/
    private String defendRowIfEnemyAttack(String command) {
        for (int i = 0; i < gameHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsInRowForPlayer(opponent.playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            if (enemyAttackOnRow > 0 && myDefenseOnRow == 0) {
                if (canAffordBuilding(BuildingType.DEFENSE)) {
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                }
                else if (canAffordBuilding(BuildingType.ENERGY)) {
                    command = placeBuildingInRowFromBack(BuildingType.ENERGY, i);
                }
                break;
            }
        }
        return command;
    }

    /**
     * If the enemy has an more than 1 attack building and I have 0 < blocking wall < 2, then block from the front.
     * @return command
     **/
    private String buildAnotherDefenceBuilding(String command) {
        for (int i = 0; i < gameHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsInRowForPlayer(opponent.playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            if (enemyAttackOnRow > 2 && myDefenseOnRow > 0 && myDefenseOnRow < 2) {
                if (canAffordBuilding(BuildingType.DEFENSE)) {
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                } else if (canAffordBuilding(BuildingType.ENERGY)) {
                    command = placeBuildingInRowFromFront(BuildingType.ENERGY, i);
                }
                break;
            }
        }
        return command;
    }

    /**
     * If there is a row where I don't have energy and there is no enemy attack building, then build energy in the back row.
     * @return command
     **/
    private String buildAttack(String command) {
        int min=0;
        command = attackMostLessHealth(command);
        if (command.equals(doNothing())) {
            for (int i=0; i < gameHeight; i++) {
                int myAttackOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
                if (myAttackOnRow == 0) {
                    if (canAffordBuilding(BuildingType.ATTACK)) {
                        command = placeBuildingInRowFromFront(BuildingType.ATTACK, i);
                    }
                    if(min<myAttackOnRow){
                        min = myAttackOnRow;
                    }
                    break;
                }
            }
            if(command.equals(doNothing())){
                for(int i=0;i<gameHeight;i++){
                    int myAttackOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
                    if(min == myAttackOnRow){
                        if(canAffordBuilding(BuildingType.ATTACK)){
                            command = placeBuildingInRowFromFront(BuildingType.ATTACK,i);
                        }
                    }
                }
            }
        }
        return command;
    }


    /**
     * If there is a row where I don't have energy and there is no enemy attack building, then build energy in the back row.
     * @return command
     **/
    private String buildEnergyIfNoEnemyAttack(String command) {
        List<Integer> safePlaceList = getSafePlace();
        Collections.shuffle(safePlaceList);
        for (int safePlace : safePlaceList) {
            int myEnergyOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ENERGY, safePlace).size();
            // int myEnergyBuilding = getAllBuildings(myself.playerType, BuildingType.ENERGY).size();
            // int opponentEnergyBuilding = getAllBuildings(opponent.playerType, BuildingType.ENERGY).size();
            if (myEnergyOnRow == 0 && myself.energy < getPriceForBuilding(BuildingType.ATTACK)) {
                if (canAffordBuilding(BuildingType.ENERGY)) 
                    command = placeBuildingInRowFromBack(BuildingType.ENERGY, safePlace);
                break;
            } else if (canAffordBuilding(BuildingType.ATTACK)) {
                command = placeBuildingInRowFromFront(BuildingType.ATTACK, safePlace);
                break;
            }
        }
        return command;
    }

    /**
     * If I have a defense building on a row, then build an attack building behind it.
     * @return command
     **/
    private String buildAttackBehindDefenceBuilding(String command) {
        for (int i = 0; i < gameHeight; i++) {
            int myDefenseOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            if (myDefenseOnRow > 0 && canAffordBuilding(BuildingType.ATTACK)) {
                command = placeBuildingInRowFromFront(BuildingType.ATTACK, i);
            }
        }
        return command;
    }
    
    /**
     * Attack most less health on a row 
     * @return command
     **/
    private String attackMostLessHealth(String command) {
        List<Integer> healthList = getHealthBuildingInRow(opponent.playerType);
        int minIndex = healthList.indexOf(Collections.min(healthList));
        if (canAffordBuilding(BuildingType.ATTACK)) {
            if(getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ATTACK, minIndex).size()<=3){
                command = placeBuildingInRowFromFront(BuildingType.ATTACK, minIndex);
            }
        }
        return command;
    }
    
    /**
     * Place building in a random row nearest to the back
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromBack(BuildingType buildingType) {
        for (int i = 0; i < gameWidth / 2; i++) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Place building in a random row nearest to the front
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromFront(BuildingType buildingType) {
        for (int i = (gameWidth / 2) - 1; i >= 0; i--) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return doNothing();
    }

    /**
     * Place building in row y nearest to the front
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
     * Place building in row y nearest to the back
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
     * Get all buildings for player in row y
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
     * Get all buildings for player in column x
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param x          the x
     * @return the result
     **/
    private List<Building> getAllBuildingsInColumnForPlayer(PlayerType playerType, Predicate<Building> filter, int x) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.x == x)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Get all empty cells for column x
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
     * Checks if cell at x,y is empty
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
     * Checks if building can be afforded
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= getPriceForBuilding(buildingType);
    }

    /**
     * Gets price for building type
     *
     * @param buildingType the player type
     * @return the result
     **/
    private int getPriceForBuilding(BuildingType buildingType) {
        return gameDetails.buildingsStats.get(buildingType).price;
    }

    /**
     * Gets price for most expensive building type
     *
     * @return the result
     **/
    private int getMostExpensiveBuildingPrice() {
        return gameDetails.buildingsStats
                .values().stream()
                .mapToInt(b -> b.price)
                .max()
                .orElse(0);
    }

    /**
     * Get random element of list
     *
     * @param list the list < t >
     * @return the result
     **/
    private <T> T getRandomElementOfList(List<T> list) {
        return list.get((new Random()).nextInt(list.size()));
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
     * Get how many buildings from player 
     *
     * @param playerType the playerType
     * @param buildingType the buildingType
     * @return the result
     **/
    private List<Building> getAllBuildings(PlayerType playerType, BuildingType buildingType) {
        List<Building> result = new ArrayList<Building>();
        for (int i = 0; i < gameHeight; i++) {
            result.addAll(getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == buildingType, i));
        }
        return result;
    }

    /**
     * Get safe place to put building 
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

    private boolean isIronCurtainAvailable() {
        return gameDetails.round % 30 == 0;
    }

    private List<Integer> getHealthBuildingInRow(PlayerType playerType) {
        int rowHealth = 0;
        List<Integer> healthList = new ArrayList<>();
        for (int i = 0; i < gameHeight; i++) {
            for (Building attackBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ATTACK, i)) {
                rowHealth += attackBuilding.health;
            }
            for (Building defenceBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.DEFENSE, i)) {
                rowHealth += defenceBuilding.health;
            }
            // for (Building energyBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ENERGY, i)) {
            //     rowHealth += energyBuilding.health;
            // }
            healthList.add(i, rowHealth);
        }
    return healthList;
    }
    
}