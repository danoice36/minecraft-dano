package org.example.dnnItem.Resource.genarator;

import org.bukkit.configuration.ConfigurationSection;
import org.example.dnnItem.DnnItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArmorGenerator {


    public static class ArmorData {

        private String id;

        private String material;

        private String layer1;

        private String layer2;

        private String color;


        public void setLayer2(@Nullable String layer2) {
            this.layer2 =  layer2;
        }

        public void setColor(@Nullable String color) {

            this.color = color;
        }

        public void setLayer1(@Nullable String layer1) {
            this.layer1 = layer1;
        }

        public void setMaterial(@Nullable String material) {

            this.material = material;
        }

        public String setId(String id) {
            return id;
        }

        public String getLayer1() {
            return layer1;
        }

        public String getLayer2() {
            return layer2;
        }

        public String getId() {
            return id;
        }
    }
    public static List<ArmorData> generate(DnnItem plugin) {

        List<ArmorData> armors = new ArrayList<>();

        ConfigurationSection section =
                plugin.getConfig()
                        .getConfigurationSection("armors_rendering");

        if (section == null)
            return armors;

        for (String id : section.getKeys(false)) {

            ConfigurationSection armor =
                    section.getConfigurationSection(id);

            ArmorData data = new ArmorData();

            data.setId(id);

            data.setMaterial(
                    armor.getString("material", "LEATHER")
            );

            data.setLayer1(
                    armor.getString("layer_1")
            );

            data.setLayer2(
                    armor.getString("layer_2")
            );

            data.setColor(
                    armor.getString("color")
            );

            armors.add(data);

        }

        return armors;

    }
}
