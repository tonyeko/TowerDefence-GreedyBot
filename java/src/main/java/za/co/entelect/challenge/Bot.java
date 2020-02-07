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
        String command = greedyAlgorithm(); // Generate command with greedy algorithm
        return command;
    }

    public String greedyAlgorithm() {
        String command = doNothing();
        // Build Energy Building di awal game selama belum di attack 
        if (command.equals(doNothing())) 
            command = buildEnergyIfNoEnemyAttack();
        // Algoritma greedy berdasarkan opponent health 
        if (command.equals(doNothing()))
            command = attackMostLessHealth();
        return command;
    }

    /**
     * If there is a row where I don't have energy and there is no enemy attack building, then build energy in the back row.
     * @return command
     **/
    private String buildEnergyIfNoEnemyAttack() {
        String command;
        List<Integer> safePlaceList = getSafePlace();
        Collections.shuffle(safePlaceList);
        for (int safePlace : safePlaceList) {
            int myEnergyOnRow = getAllBuildingsInRowForPlayer(myself.playerType, b -> b.buildingType == BuildingType.ENERGY, safePlace).size();
            if (myEnergyOnRow == 0 && myself.energy < getPriceForBuilding(BuildingType.ATTACK)) {
                if (canAffordBuilding(BuildingType.ENERGY)) 
                    command = placeBuildingInRowFromBack(BuildingType.ENERGY, safePlace);
                break;
            } 
        }
        return command;
    }
    
    /**
     * Attack most less health on a row 
     * @return command
     **/
    private String attackMostLessHealth() {
        String command;
        List<Integer> healthList = getHealthBuildingInRow(opponent.playerType);
        int minIndex = healthList.indexOf(Collections.min(healthList));
        List<Integer> minIndexList = getListOfMinHealthIndex(healthList);
        if (minIndexList.size() > 1) {
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
        if (canAffordBuilding(BuildingType.ATTACK)) {
            command = placeBuildingInRowFromFront(BuildingType.ATTACK, minIndex);
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
     * Do nothing command
     *
     * @return the result
     **/
    private String doNothing() {
        return "";
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

    private List<Integer> getHealthBuildingInRow(PlayerType playerType) {
        List<Integer> healthList = new ArrayList<>();
        for (int i = 0; i < gameHeight; i++) {
            int rowHealth = 0;
            int enemyAttackOnRow = getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int enemyEnergyOnRow = getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ENERGY, i).size();
            for (Building attackBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ATTACK, i)) {
                if (enemyAttackOnRow > 1) {
                    rowHealth += attackBuilding.health;
                } else {
                    rowHealth -= attackBuilding.health;
                }
            }
            for (Building defenceBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.DEFENSE, i)) {
                rowHealth += defenceBuilding.health*100;
            }
            for (Building energyBuilding : getAllBuildingsInRowForPlayer(playerType, b -> b.buildingType == BuildingType.ENERGY, i)) {
                if (enemyEnergyOnRow == 1) {
                    rowHealth -= energyBuilding.health*100;
                }
            }
            healthList.add(i, rowHealth);
        }
        return healthList;
    }
    
    private List<Integer> getListOfMinHealthIndex(List<Integer> healthList) {
        List<Integer> minHealthIndexList = new ArrayList<>();
        int minHealth = Collections.min(healthList);
        for (int i = 0; i < healthList.size(); i++) {
            if (healthList.get(i) == minHealth) {
                minHealthIndexList.add(i);
            }
        }
        return minHealthIndexList;
    }
}