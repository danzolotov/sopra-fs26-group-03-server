package ch.uzh.ifi.hase.soprafs26.constant;

import java.util.List;

public class IngredientSeedData {
    public static final List<IngredientData> INGREDIENTS = List.of(
            new IngredientData("Onion", List.of(), "Vegetable"),
            new IngredientData("Garlic", List.of(), "Vegetable"),
            new IngredientData("Tomato", List.of(), "Vegetable"),
            new IngredientData("Potato", List.of(), "Vegetable"),
            new IngredientData("Carrot", List.of(), "Vegetable"),
            new IngredientData("Bell Pepper", List.of("Capsicum"), "Vegetable"),
            new IngredientData("Cucumber", List.of(), "Vegetable"),
            new IngredientData("Zucchini", List.of("Courgette"), "Vegetable"),
            new IngredientData("Broccoli", List.of(), "Vegetable"),
            new IngredientData("Spinach", List.of(), "Vegetable"),
            new IngredientData("Lettuce", List.of(), "Vegetable"),
            new IngredientData("Mushroom", List.of("Champignon"), "Vegetable"),
            new IngredientData("Corn", List.of("Sweetcorn"), "Vegetable"),
            new IngredientData("Peas", List.of("Green Peas"), "Vegetable"),
            new IngredientData("Eggplant", List.of("Aubergine"), "Vegetable"),
            new IngredientData("Avocado", List.of(), "Vegetable"),
            new IngredientData("Celery", List.of(), "Vegetable"),
            new IngredientData("Leek", List.of(), "Vegetable"),
            new IngredientData("Sweet Potato", List.of(), "Vegetable"),
            new IngredientData("Pumpkin", List.of(), "Vegetable"),
            new IngredientData("Cauliflower", List.of(), "Vegetable"),
            new IngredientData("Cabbage", List.of(), "Vegetable"),
            new IngredientData("Green Beans", List.of("String Beans"), "Vegetable"),
            new IngredientData("Chili Pepper", List.of("Chili", "Chilli"), "Vegetable"),
            new IngredientData("Radish", List.of(), "Vegetable"),

            new IngredientData("Apple", List.of(), "Fruit"),
            new IngredientData("Banana", List.of(), "Fruit"),
            new IngredientData("Lemon", List.of(), "Fruit"),
            new IngredientData("Lime", List.of(), "Fruit"),
            new IngredientData("Orange", List.of(), "Fruit"),
            new IngredientData("Strawberry", List.of(), "Fruit"),
            new IngredientData("Blueberry", List.of(), "Fruit"),
            new IngredientData("Raspberry", List.of(), "Fruit"),
            new IngredientData("Mango", List.of(), "Fruit"),
            new IngredientData("Pineapple", List.of(), "Fruit"),
            new IngredientData("Pear", List.of(), "Fruit"),
            new IngredientData("Peach", List.of(), "Fruit"),
            new IngredientData("Grapes", List.of(), "Fruit"),
            new IngredientData("Coconut", List.of(), "Fruit"),
            new IngredientData("Watermelon", List.of(), "Fruit"),

            new IngredientData("Chicken Breast", List.of(), "Meat"),
            new IngredientData("Chicken Thigh", List.of(), "Meat"),
            new IngredientData("Ground Beef", List.of("Minced Beef", "Beef Mince"), "Meat"),
            new IngredientData("Beef Steak", List.of("Steak"), "Meat"),
            new IngredientData("Pork Chop", List.of(), "Meat"),
            new IngredientData("Ground Pork", List.of("Minced Pork", "Pork Mince"), "Meat"),
            new IngredientData("Bacon", List.of(), "Meat"),
            new IngredientData("Sausage", List.of(), "Meat"),
            new IngredientData("Ham", List.of(), "Meat"),
            new IngredientData("Turkey Breast", List.of(), "Meat"),

            new IngredientData("Salmon", List.of(), "Fish"),
            new IngredientData("Tuna", List.of(), "Fish"),
            new IngredientData("Shrimp", List.of("Prawns"), "Fish"),
            new IngredientData("Cod", List.of(), "Fish"),
            new IngredientData("Trout", List.of(), "Fish"),
            new IngredientData("Sardines", List.of(), "Fish"),

            new IngredientData("Milk", List.of(), "Dairy"),
            new IngredientData("Butter", List.of(), "Dairy"),
            new IngredientData("Cream", List.of(), "Dairy"),
            new IngredientData("Yogurt", List.of("Yoghurt"), "Dairy"),
            new IngredientData("Greek Yogurt", List.of("Greek Yoghurt"), "Dairy"),
            new IngredientData("Sour Cream", List.of(), "Dairy"),
            new IngredientData("Cream Cheese", List.of(), "Dairy"),
            new IngredientData("Parmesan", List.of("Parmigiano"), "Dairy"),
            new IngredientData("Mozzarella", List.of(), "Dairy"),
            new IngredientData("Cheddar", List.of(), "Dairy"),
            new IngredientData("Feta", List.of(), "Dairy"),
            new IngredientData("Cottage Cheese", List.of(), "Dairy"),

            new IngredientData("Egg", List.of(), "Eggs"),

            new IngredientData("Tofu", List.of(), "Plant Protein"),
            new IngredientData("Lentils", List.of(), "Plant Protein"),
            new IngredientData("Chickpeas", List.of("Garbanzo Beans"), "Plant Protein"),
            new IngredientData("Black Beans", List.of(), "Plant Protein"),
            new IngredientData("Kidney Beans", List.of(), "Plant Protein"),

            new IngredientData("Rice", List.of(), "Grain"),
            new IngredientData("Pasta", List.of(), "Grain"),
            new IngredientData("Spaghetti", List.of(), "Grain"),
            new IngredientData("Noodles", List.of(), "Grain"),
            new IngredientData("Bread", List.of(), "Bakery"),
            new IngredientData("Flour", List.of(), "Baking"),
            new IngredientData("Oats", List.of(), "Grain"),
            new IngredientData("Quinoa", List.of(), "Grain"),
            new IngredientData("Couscous", List.of(), "Grain"),
            new IngredientData("Tortilla", List.of("Wrap"), "Bakery"),

            new IngredientData("Parsley", List.of(), "Herb"),
            new IngredientData("Basil", List.of(), "Herb"),
            new IngredientData("Cilantro", List.of("Coriander"), "Herb"),
            new IngredientData("Mint", List.of(), "Herb"),
            new IngredientData("Dill", List.of(), "Herb"),
            new IngredientData("Rosemary", List.of(), "Herb"),
            new IngredientData("Thyme", List.of(), "Herb"),
            new IngredientData("Oregano", List.of(), "Herb"),

            new IngredientData("Salt", List.of(), "Spice"),
            new IngredientData("Black Pepper", List.of("Pepper"), "Spice"),
            new IngredientData("Paprika", List.of(), "Spice"),
            new IngredientData("Chili Powder", List.of(), "Spice"),
            new IngredientData("Cumin", List.of(), "Spice"),
            new IngredientData("Curry Powder", List.of(), "Spice"),
            new IngredientData("Turmeric", List.of(), "Spice"),
            new IngredientData("Cinnamon", List.of(), "Spice"),
            new IngredientData("Nutmeg", List.of(), "Spice"),
            new IngredientData("Ginger", List.of(), "Spice"),

            new IngredientData("Olive Oil", List.of(), "Oil"),
            new IngredientData("Vegetable Oil", List.of(), "Oil"),
            new IngredientData("Sesame Oil", List.of(), "Oil"),
            new IngredientData("Soy Sauce", List.of(), "Condiment"),
            new IngredientData("Vinegar", List.of(), "Condiment"),
            new IngredientData("Balsamic Vinegar", List.of(), "Condiment"),
            new IngredientData("Mustard", List.of(), "Condiment"),
            new IngredientData("Mayonnaise", List.of(), "Condiment"),
            new IngredientData("Ketchup", List.of(), "Condiment"),
            new IngredientData("Honey", List.of(), "Condiment"),

            new IngredientData("Sugar", List.of(), "Baking"),
            new IngredientData("Brown Sugar", List.of(), "Baking"),
            new IngredientData("Baking Powder", List.of(), "Baking"),
            new IngredientData("Baking Soda", List.of(), "Baking"),
            new IngredientData("Vanilla Extract", List.of(), "Baking")
    );
    public static class IngredientData {

    private final String name;
    private final List<String> aliases;
    private final String category;

    public IngredientData(String name, List<String> aliases, String category) {
        this.name = name;
        this.aliases = aliases;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getCategory() {
        return category;
    }
}

}

