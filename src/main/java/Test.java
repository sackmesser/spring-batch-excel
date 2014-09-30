import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.excel.RowCallbackHandler;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.excel.mapping.DefaultRowMapper;
import org.springframework.batch.item.excel.poi.PoiItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * Created by fvalmeida on 9/9/14.
 */
public class Test {

    public static void main(String[] args) throws Exception {

        PoiItemReader<Player> itemReader = new PoiItemReader();
        itemReader.setLinesToSkip(1); //First line is column names
//        itemReader.setResource(new ClassPathResource("/Player.xls"));
        itemReader.setResource(new ClassPathResource("/Player.xlsx"));
        DefaultRowMapper<Player> defaultRowMapper = new DefaultRowMapper(Player.class);
//        defaultRowMapper.setFieldSetMapper(new PlayerMapper());
        BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
        mapper.setTargetType(Player.class);
        mapper.setStrict(false);
        mapper.setDistanceLimit(0);
        defaultRowMapper.setFieldSetMapper(mapper);

                itemReader.setRowMapper(new DefaultRowMapper(Player.class));
//                itemReader.setRowMapper(defaultRowMapper);
        itemReader.setSkippedRowsCallback(new RowCallbackHandler() {
            public void handleRow(final Sheet sheet, final String[] row) {
                System.out.println("Skipping: " + StringUtils.arrayToCommaDelimitedString(row));
            }
        });
        itemReader.afterPropertiesSet();
        itemReader.open(new ExecutionContext());
        Player row;
        do {
            row = itemReader.read();
            if (row != null)
                System.out.println("Read: " + row.toString());
        } while (row != null);
    }

    @Setter
    @Getter
    @ToString
    public static class Player implements Serializable {

        private String id;
        //private String asdf;
        private String lastName;
        private String firstName;
        private String position;
        private int birthYear;
        private int debutYear;

    }

    public static class PlayerMapper implements FieldSetMapper<Player> {
        public Player mapFieldSet(FieldSet fs) {

            if (fs == null) {
                return null;
            }

            Player player = new Player();
            player.setId(fs.readString("id"));
            player.setLastName(fs.readString("lastName"));
            player.setFirstName(fs.readString("firstName"));
            player.setPosition(fs.readString("position"));
            player.setDebutYear(fs.readInt("debutYear"));
            player.setBirthYear(fs.readInt("birthYear"));

            return player;
        }
    }
}
