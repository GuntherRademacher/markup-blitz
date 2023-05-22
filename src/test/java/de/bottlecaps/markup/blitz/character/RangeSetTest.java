package de.bottlecaps.markup.blitz.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class RangeSetTest {

  @Test
  public void testRangeSet() {
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 2));
      assertEquals("[#1-#2; #4-#5]", set.toString());
      assertEquals("[#1-#2; #4-#5]", set.split().toString());
      assertEquals("[#1-#2; #4-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(4, 5));
      assertEquals("[#1-#2; #4-#5]", set.toString());
      assertEquals("[#1-#2; #4-#5]", set.split().toString());
      assertEquals("[#1-#2; #4-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(4, 5));
      assertEquals("[#1-#3; #4-#5]", set.toString());
      assertEquals("[#1-#3; #4-#5]", set.split().toString());
      assertEquals("[#1-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 3));
      assertEquals("[#1-#3; #4-#5]", set.toString());
      assertEquals("[#1-#3; #4-#5]", set.split().toString());
      assertEquals("[#1-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(3, 5));
      assertEquals("[#1-#3; #3-#5]", set.toString());
      assertEquals("[#1-#2; #3; #4-#5]", set.split().toString());
      assertEquals("[#1-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 5),
        new Range(1, 3));
      assertEquals("[#1-#3; #3-#5]", set.toString());
      assertEquals("[#1-#2; #3; #4-#5]", set.split().toString());
      assertEquals("[#1-#5]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(3, 4));
      assertEquals("[#1-#2; #3-#4]", set.toString());
      assertEquals("[#1-#2; #3-#4]", set.split().toString());
      assertEquals("[#1-#4]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 4),
        new Range(1, 2));
      assertEquals("[#1-#2; #3-#4]", set.toString());
      assertEquals("[#1-#2; #3-#4]", set.split().toString());
      assertEquals("[#1-#4]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 10),
        new Range(2, 3));
      assertEquals("[#1-#a; #2-#3]", set.toString());
      assertEquals("[#1; #2-#3; #4-#a]", set.split().toString());
      assertEquals("[#1-#a]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(2, 3),
        new Range(1, 10));
      assertEquals("[#1-#a; #2-#3]", set.toString());
      assertEquals("[#1; #2-#3; #4-#a]", set.split().toString());
      assertEquals("[#1-#a]", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('p', 't'));
      assertEquals("['a'-'z'; 'c'; 'n'-'q'; 'p'-'t']", set.toString());
      assertEquals("['a'-'b'; 'c'; 'd'-'m'; 'n'-'o'; 'p'-'q'; 'r'-'t'; 'u'-'z']", set.split().toString());
      assertEquals("['a'-'z']", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('p', 't'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('a', 'z'));
      assertEquals("['a'-'z'; 'c'; 'n'-'q'; 'p'-'t']", set.toString());
      assertEquals("['a'-'b'; 'c'; 'd'-'m'; 'n'-'o'; 'p'-'q'; 'r'-'t'; 'u'-'z']", set.split().toString());
      assertEquals("['a'-'z']", set.join().toString());
      assertEquals(set.join().toString(), set.join().split().toString());
      assertEquals(set.join().toString(), set.split().join().toString());
    }
  }

  @Test
  public void testMinus() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .minus(RangeSet.of(new Range('k'), new Range('p', 'r')));
      assertEquals("['a'-'j'; 'l'-'o'; 's'-'z']", set.toString());
    }
  }

  @Test
  public void testComplement() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .complement();
      assertEquals("[#9-#a; #d; ' '-'`'; '{'-#d7ff; #e000-#fffd; #10000-#10fffd]", set.toString());
      assertEquals("['a'-'z']", set.complement().toString());
    }
  }

  @Test
  public void testRandomInsertions() {
    Random random = new Random();
    long seed = random.nextLong();
    random.setSeed(seed);
    String msgPrefix = "With seed " + seed + "L: ";
    for (int iteration = 0; iteration < 1024; ++iteration) {
      int numberOfRanges = 1 + random.nextInt(16); // .nextInt(100);
      List<Range> ranges = new ArrayList<>();
      for (int r = 0; r < numberOfRanges; ++r) {
        int f = ' ' + random.nextInt(96);
        int l = f + random.nextInt(128 - f);
        Range range = new Range(f, l);
        ranges.add(range);
      }
      String string = null;
      String splitString = null;
      String joinString = null;
      String complementString = null;
      for (int permutation = 0; permutation < 100; ++permutation) {
        RangeSet.Builder builder = new RangeSet.Builder();
        Collections.shuffle(ranges);
        for (Range range : ranges)
          builder.add(range);
        RangeSet set = builder.build();
        RangeSet complement = set.complement();
        if (string == null) {
          string = set.toString();
          splitString = set.split().toString();
          joinString = set.join().toString();
          complementString = complement.toString();
        }
        else {
          assertEquals(string, set.toString(), msgPrefix + "unexpected result");
          assertEquals(splitString, set.split().toString(), msgPrefix + "unexpected result");
          assertEquals(joinString, set.join().toString(), msgPrefix + "unexpected result");
          assertEquals(complementString, complement.toString(), msgPrefix + "unexpected result");
          assertEquals(joinString, complement.complement().toString(), msgPrefix + "unexpected result");
        }
      }
    }
  }

  @Test
  public void testOfUnicodeCharClass() {
    assertEquals("[' '; #a0; #1680; #2000-#200a; #202f; #205f; #3000]", RangeSet.of("Zs").toString());
    assertEquals("['A'-'Z'; 'a'-'z'; #aa; #b5; #ba; #c0-#d6; #d8-#f6; #f8-#2c1; #2c6-#2d1; #2e0-#2e4; #2ec; #2ee; #370-#374; #376-#377; #37a-#37d; #37f; #386; #388-#38a; #38c; #38e-#3a1; #3a3-#3f5; #3f7-#481; #48a-#52f; #531-#556; #559; #561-#587; #5d0-#5ea; #5f0-#5f2; #620-#64a; #66e-#66f; #671-#6d3; #6d5; #6e5-#6e6; #6ee-#6ef; #6fa-#6fc; #6ff; #710; #712-#72f; #74d-#7a5; #7b1; #7ca-#7ea; #7f4-#7f5; #7fa; #800-#815; #81a; #824; #828; #840-#858; #860-#86a; #8a0-#8b4; #8b6-#8bd; #904-#939; #93d; #950; #958-#961; #971-#980; #985-#98c; #98f-#990; #993-#9a8; #9aa-#9b0; #9b2; #9b6-#9b9; #9bd; #9ce; #9dc-#9dd; #9df-#9e1; #9f0-#9f1; #9fc; #a05-#a0a; #a0f-#a10; #a13-#a28; #a2a-#a30; #a32-#a33; #a35-#a36; #a38-#a39; #a59-#a5c; #a5e; #a72-#a74; #a85-#a8d; #a8f-#a91; #a93-#aa8; #aaa-#ab0; #ab2-#ab3; #ab5-#ab9; #abd; #ad0; #ae0-#ae1; #af9; #b05-#b0c; #b0f-#b10; #b13-#b28; #b2a-#b30; #b32-#b33; #b35-#b39; #b3d; #b5c-#b5d; #b5f-#b61; #b71; #b83; #b85-#b8a; #b8e-#b90; #b92-#b95; #b99-#b9a; #b9c; #b9e-#b9f; #ba3-#ba4; #ba8-#baa; #bae-#bb9; #bd0; #c05-#c0c; #c0e-#c10; #c12-#c28; #c2a-#c39; #c3d; #c58-#c5a; #c60-#c61; #c80; #c85-#c8c; #c8e-#c90; #c92-#ca8; #caa-#cb3; #cb5-#cb9; #cbd; #cde; #ce0-#ce1; #cf1-#cf2; #d05-#d0c; #d0e-#d10; #d12-#d3a; #d3d; #d4e; #d54-#d56; #d5f-#d61; #d7a-#d7f; #d85-#d96; #d9a-#db1; #db3-#dbb; #dbd; #dc0-#dc6; #e01-#e30; #e32-#e33; #e40-#e46; #e81-#e82; #e84; #e87-#e88; #e8a; #e8d; #e94-#e97; #e99-#e9f; #ea1-#ea3; #ea5; #ea7; #eaa-#eab; #ead-#eb0; #eb2-#eb3; #ebd; #ec0-#ec4; #ec6; #edc-#edf; #f00; #f40-#f47; #f49-#f6c; #f88-#f8c; #1000-#102a; #103f; #1050-#1055; #105a-#105d; #1061; #1065-#1066; #106e-#1070; #1075-#1081; #108e; #10a0-#10c5; #10c7; #10cd; #10d0-#10fa; #10fc-#1248; #124a-#124d; #1250-#1256; #1258; #125a-#125d; #1260-#1288; #128a-#128d; #1290-#12b0; #12b2-#12b5; #12b8-#12be; #12c0; #12c2-#12c5; #12c8-#12d6; #12d8-#1310; #1312-#1315; #1318-#135a; #1380-#138f; #13a0-#13f5; #13f8-#13fd; #1401-#166c; #166f-#167f; #1681-#169a; #16a0-#16ea; #16f1-#16f8; #1700-#170c; #170e-#1711; #1720-#1731; #1740-#1751; #1760-#176c; #176e-#1770; #1780-#17b3; #17d7; #17dc; #1820-#1877; #1880-#1884; #1887-#18a8; #18aa; #18b0-#18f5; #1900-#191e; #1950-#196d; #1970-#1974; #1980-#19ab; #19b0-#19c9; #1a00-#1a16; #1a20-#1a54; #1aa7; #1b05-#1b33; #1b45-#1b4b; #1b83-#1ba0; #1bae-#1baf; #1bba-#1be5; #1c00-#1c23; #1c4d-#1c4f; #1c5a-#1c7d; #1c80-#1c88; #1ce9-#1cec; #1cee-#1cf1; #1cf5-#1cf6; #1d00-#1dbf; #1e00-#1f15; #1f18-#1f1d; #1f20-#1f45; #1f48-#1f4d; #1f50-#1f57; #1f59; #1f5b; #1f5d; #1f5f-#1f7d; #1f80-#1fb4; #1fb6-#1fbc; #1fbe; #1fc2-#1fc4; #1fc6-#1fcc; #1fd0-#1fd3; #1fd6-#1fdb; #1fe0-#1fec; #1ff2-#1ff4; #1ff6-#1ffc; #2071; #207f; #2090-#209c; #2102; #2107; #210a-#2113; #2115; #2119-#211d; #2124; #2126; #2128; #212a-#212d; #212f-#2139; #213c-#213f; #2145-#2149; #214e; #2183-#2184; #2c00-#2c2e; #2c30-#2c5e; #2c60-#2ce4; #2ceb-#2cee; #2cf2-#2cf3; #2d00-#2d25; #2d27; #2d2d; #2d30-#2d67; #2d6f; #2d80-#2d96; #2da0-#2da6; #2da8-#2dae; #2db0-#2db6; #2db8-#2dbe; #2dc0-#2dc6; #2dc8-#2dce; #2dd0-#2dd6; #2dd8-#2dde; #2e2f; #3005-#3006; #3031-#3035; #303b-#303c; #3041-#3096; #309d-#309f; #30a1-#30fa; #30fc-#30ff; #3105-#312e; #3131-#318e; #31a0-#31ba; #31f0-#31ff; #3400-#4db5; #4e00-#9fea; #a000-#a48c; #a4d0-#a4fd; #a500-#a60c; #a610-#a61f; #a62a-#a62b; #a640-#a66e; #a67f-#a69d; #a6a0-#a6e5; #a717-#a71f; #a722-#a788; #a78b-#a7ae; #a7b0-#a7b7; #a7f7-#a801; #a803-#a805; #a807-#a80a; #a80c-#a822; #a840-#a873; #a882-#a8b3; #a8f2-#a8f7; #a8fb; #a8fd; #a90a-#a925; #a930-#a946; #a960-#a97c; #a984-#a9b2; #a9cf; #a9e0-#a9e4; #a9e6-#a9ef; #a9fa-#a9fe; #aa00-#aa28; #aa40-#aa42; #aa44-#aa4b; #aa60-#aa76; #aa7a; #aa7e-#aaaf; #aab1; #aab5-#aab6; #aab9-#aabd; #aac0; #aac2; #aadb-#aadd; #aae0-#aaea; #aaf2-#aaf4; #ab01-#ab06; #ab09-#ab0e; #ab11-#ab16; #ab20-#ab26; #ab28-#ab2e; #ab30-#ab5a; #ab5c-#ab65; #ab70-#abe2; #ac00-#d7a3; #d7b0-#d7c6; #d7cb-#d7fb; #f900-#fa6d; #fa70-#fad9; #fb00-#fb06; #fb13-#fb17; #fb1d; #fb1f-#fb28; #fb2a-#fb36; #fb38-#fb3c; #fb3e; #fb40-#fb41; #fb43-#fb44; #fb46-#fbb1; #fbd3-#fd3d; #fd50-#fd8f; #fd92-#fdc7; #fdf0-#fdfb; #fe70-#fe74; #fe76-#fefc; #ff21-#ff3a; #ff41-#ff5a; #ff66-#ffbe; #ffc2-#ffc7; #ffca-#ffcf; #ffd2-#ffd7; #ffda-#ffdc; #10000-#1000b; #1000d-#10026; #10028-#1003a; #1003c-#1003d; #1003f-#1004d; #10050-#1005d; #10080-#100fa; #10280-#1029c; #102a0-#102d0; #10300-#1031f; #1032d-#10340; #10342-#10349; #10350-#10375; #10380-#1039d; #103a0-#103c3; #103c8-#103cf; #10400-#1049d; #104b0-#104d3; #104d8-#104fb; #10500-#10527; #10530-#10563; #10600-#10736; #10740-#10755; #10760-#10767; #10800-#10805; #10808; #1080a-#10835; #10837-#10838; #1083c; #1083f-#10855; #10860-#10876; #10880-#1089e; #108e0-#108f2; #108f4-#108f5; #10900-#10915; #10920-#10939; #10980-#109b7; #109be-#109bf; #10a00; #10a10-#10a13; #10a15-#10a17; #10a19-#10a33; #10a60-#10a7c; #10a80-#10a9c; #10ac0-#10ac7; #10ac9-#10ae4; #10b00-#10b35; #10b40-#10b55; #10b60-#10b72; #10b80-#10b91; #10c00-#10c48; #10c80-#10cb2; #10cc0-#10cf2; #11003-#11037; #11083-#110af; #110d0-#110e8; #11103-#11126; #11150-#11172; #11176; #11183-#111b2; #111c1-#111c4; #111da; #111dc; #11200-#11211; #11213-#1122b; #11280-#11286; #11288; #1128a-#1128d; #1128f-#1129d; #1129f-#112a8; #112b0-#112de; #11305-#1130c; #1130f-#11310; #11313-#11328; #1132a-#11330; #11332-#11333; #11335-#11339; #1133d; #11350; #1135d-#11361; #11400-#11434; #11447-#1144a; #11480-#114af; #114c4-#114c5; #114c7; #11580-#115ae; #115d8-#115db; #11600-#1162f; #11644; #11680-#116aa; #11700-#11719; #118a0-#118df; #118ff; #11a00; #11a0b-#11a32; #11a3a; #11a50; #11a5c-#11a83; #11a86-#11a89; #11ac0-#11af8; #11c00-#11c08; #11c0a-#11c2e; #11c40; #11c72-#11c8f; #11d00-#11d06; #11d08-#11d09; #11d0b-#11d30; #11d46; #12000-#12399; #12480-#12543; #13000-#1342e; #14400-#14646; #16800-#16a38; #16a40-#16a5e; #16ad0-#16aed; #16b00-#16b2f; #16b40-#16b43; #16b63-#16b77; #16b7d-#16b8f; #16f00-#16f44; #16f50; #16f93-#16f9f; #16fe0-#16fe1; #17000-#187ec; #18800-#18af2; #1b000-#1b11e; #1b170-#1b2fb; #1bc00-#1bc6a; #1bc70-#1bc7c; #1bc80-#1bc88; #1bc90-#1bc99; #1d400-#1d454; #1d456-#1d49c; #1d49e-#1d49f; #1d4a2; #1d4a5-#1d4a6; #1d4a9-#1d4ac; #1d4ae-#1d4b9; #1d4bb; #1d4bd-#1d4c3; #1d4c5-#1d505; #1d507-#1d50a; #1d50d-#1d514; #1d516-#1d51c; #1d51e-#1d539; #1d53b-#1d53e; #1d540-#1d544; #1d546; #1d54a-#1d550; #1d552-#1d6a5; #1d6a8-#1d6c0; #1d6c2-#1d6da; #1d6dc-#1d6fa; #1d6fc-#1d714; #1d716-#1d734; #1d736-#1d74e; #1d750-#1d76e; #1d770-#1d788; #1d78a-#1d7a8; #1d7aa-#1d7c2; #1d7c4-#1d7cb; #1e800-#1e8c4; #1e900-#1e943; #1ee00-#1ee03; #1ee05-#1ee1f; #1ee21-#1ee22; #1ee24; #1ee27; #1ee29-#1ee32; #1ee34-#1ee37; #1ee39; #1ee3b; #1ee42; #1ee47; #1ee49; #1ee4b; #1ee4d-#1ee4f; #1ee51-#1ee52; #1ee54; #1ee57; #1ee59; #1ee5b; #1ee5d; #1ee5f; #1ee61-#1ee62; #1ee64; #1ee67-#1ee6a; #1ee6c-#1ee72; #1ee74-#1ee77; #1ee79-#1ee7c; #1ee7e; #1ee80-#1ee89; #1ee8b-#1ee9b; #1eea1-#1eea3; #1eea5-#1eea9; #1eeab-#1eebb; #20000-#2a6d6; #2a700-#2b734; #2b740-#2b81d; #2b820-#2cea1; #2ceb0-#2ebe0; #2f800-#2fa1d]", RangeSet.of("L").toString());
    assertEquals("['0'-'9'; #660-#669; #6f0-#6f9; #7c0-#7c9; #966-#96f; #9e6-#9ef; #a66-#a6f; #ae6-#aef; #b66-#b6f; #be6-#bef; #c66-#c6f; #ce6-#cef; #d66-#d6f; #de6-#def; #e50-#e59; #ed0-#ed9; #f20-#f29; #1040-#1049; #1090-#1099; #17e0-#17e9; #1810-#1819; #1946-#194f; #19d0-#19d9; #1a80-#1a89; #1a90-#1a99; #1b50-#1b59; #1bb0-#1bb9; #1c40-#1c49; #1c50-#1c59; #a620-#a629; #a8d0-#a8d9; #a900-#a909; #a9d0-#a9d9; #a9f0-#a9f9; #aa50-#aa59; #abf0-#abf9; #ff10-#ff19; #104a0-#104a9; #11066-#1106f; #110f0-#110f9; #11136-#1113f; #111d0-#111d9; #112f0-#112f9; #11450-#11459; #114d0-#114d9; #11650-#11659; #116c0-#116c9; #11730-#11739; #118e0-#118e9; #11c50-#11c59; #11d50-#11d59; #16a60-#16a69; #16b50-#16b59; #1d7ce-#1d7ff; #1e950-#1e959]", RangeSet.of("Nd").toString());
    assertEquals("[#300-#36f; #483-#487; #591-#5bd; #5bf; #5c1-#5c2; #5c4-#5c5; #5c7; #610-#61a; #64b-#65f; #670; #6d6-#6dc; #6df-#6e4; #6e7-#6e8; #6ea-#6ed; #711; #730-#74a; #7a6-#7b0; #7eb-#7f3; #816-#819; #81b-#823; #825-#827; #829-#82d; #859-#85b; #8d4-#8e1; #8e3-#902; #93a; #93c; #941-#948; #94d; #951-#957; #962-#963; #981; #9bc; #9c1-#9c4; #9cd; #9e2-#9e3; #a01-#a02; #a3c; #a41-#a42; #a47-#a48; #a4b-#a4d; #a51; #a70-#a71; #a75; #a81-#a82; #abc; #ac1-#ac5; #ac7-#ac8; #acd; #ae2-#ae3; #afa-#aff; #b01; #b3c; #b3f; #b41-#b44; #b4d; #b56; #b62-#b63; #b82; #bc0; #bcd; #c00; #c3e-#c40; #c46-#c48; #c4a-#c4d; #c55-#c56; #c62-#c63; #c81; #cbc; #cbf; #cc6; #ccc-#ccd; #ce2-#ce3; #d00-#d01; #d3b-#d3c; #d41-#d44; #d4d; #d62-#d63; #dca; #dd2-#dd4; #dd6; #e31; #e34-#e3a; #e47-#e4e; #eb1; #eb4-#eb9; #ebb-#ebc; #ec8-#ecd; #f18-#f19; #f35; #f37; #f39; #f71-#f7e; #f80-#f84; #f86-#f87; #f8d-#f97; #f99-#fbc; #fc6; #102d-#1030; #1032-#1037; #1039-#103a; #103d-#103e; #1058-#1059; #105e-#1060; #1071-#1074; #1082; #1085-#1086; #108d; #109d; #135d-#135f; #1712-#1714; #1732-#1734; #1752-#1753; #1772-#1773; #17b4-#17b5; #17b7-#17bd; #17c6; #17c9-#17d3; #17dd; #180b-#180d; #1885-#1886; #18a9; #1920-#1922; #1927-#1928; #1932; #1939-#193b; #1a17-#1a18; #1a1b; #1a56; #1a58-#1a5e; #1a60; #1a62; #1a65-#1a6c; #1a73-#1a7c; #1a7f; #1ab0-#1abd; #1b00-#1b03; #1b34; #1b36-#1b3a; #1b3c; #1b42; #1b6b-#1b73; #1b80-#1b81; #1ba2-#1ba5; #1ba8-#1ba9; #1bab-#1bad; #1be6; #1be8-#1be9; #1bed; #1bef-#1bf1; #1c2c-#1c33; #1c36-#1c37; #1cd0-#1cd2; #1cd4-#1ce0; #1ce2-#1ce8; #1ced; #1cf4; #1cf8-#1cf9; #1dc0-#1df9; #1dfb-#1dff; #20d0-#20dc; #20e1; #20e5-#20f0; #2cef-#2cf1; #2d7f; #2de0-#2dff; #302a-#302d; #3099-#309a; #a66f; #a674-#a67d; #a69e-#a69f; #a6f0-#a6f1; #a802; #a806; #a80b; #a825-#a826; #a8c4-#a8c5; #a8e0-#a8f1; #a926-#a92d; #a947-#a951; #a980-#a982; #a9b3; #a9b6-#a9b9; #a9bc; #a9e5; #aa29-#aa2e; #aa31-#aa32; #aa35-#aa36; #aa43; #aa4c; #aa7c; #aab0; #aab2-#aab4; #aab7-#aab8; #aabe-#aabf; #aac1; #aaec-#aaed; #aaf6; #abe5; #abe8; #abed; #fb1e; #fe00-#fe0f; #fe20-#fe2f; #101fd; #102e0; #10376-#1037a; #10a01-#10a03; #10a05-#10a06; #10a0c-#10a0f; #10a38-#10a3a; #10a3f; #10ae5-#10ae6; #11001; #11038-#11046; #1107f-#11081; #110b3-#110b6; #110b9-#110ba; #11100-#11102; #11127-#1112b; #1112d-#11134; #11173; #11180-#11181; #111b6-#111be; #111ca-#111cc; #1122f-#11231; #11234; #11236-#11237; #1123e; #112df; #112e3-#112ea; #11300-#11301; #1133c; #11340; #11366-#1136c; #11370-#11374; #11438-#1143f; #11442-#11444; #11446; #114b3-#114b8; #114ba; #114bf-#114c0; #114c2-#114c3; #115b2-#115b5; #115bc-#115bd; #115bf-#115c0; #115dc-#115dd; #11633-#1163a; #1163d; #1163f-#11640; #116ab; #116ad; #116b0-#116b5; #116b7; #1171d-#1171f; #11722-#11725; #11727-#1172b; #11a01-#11a06; #11a09-#11a0a; #11a33-#11a38; #11a3b-#11a3e; #11a47; #11a51-#11a56; #11a59-#11a5b; #11a8a-#11a96; #11a98-#11a99; #11c30-#11c36; #11c38-#11c3d; #11c3f; #11c92-#11ca7; #11caa-#11cb0; #11cb2-#11cb3; #11cb5-#11cb6; #11d31-#11d36; #11d3a; #11d3c-#11d3d; #11d3f-#11d45; #11d47; #16af0-#16af4; #16b30-#16b36; #16f8f-#16f92; #1bc9d-#1bc9e; #1d167-#1d169; #1d17b-#1d182; #1d185-#1d18b; #1d1aa-#1d1ad; #1d242-#1d244; #1da00-#1da36; #1da3b-#1da6c; #1da75; #1da84; #1da9b-#1da9f; #1daa1-#1daaf; #1e000-#1e006; #1e008-#1e018; #1e01b-#1e021; #1e023-#1e024; #1e026-#1e02a; #1e8d0-#1e8d6; #1e944-#1e94a; #e0100-#e01ef]", RangeSet.of("Mn").toString());
  }
}
