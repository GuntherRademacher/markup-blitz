package de.bottlecaps.markup.blitz.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.bottlecaps.markup.TestBase;

public class RangeSetTest extends TestBase {

  @Test
  public void testRangeSet() {
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 2));
      assertEquals("[#1-#2; #4-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(4, 5));
      assertEquals("[#1-#2; #4-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(4, 5));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 3));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(3, 5));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 5),
        new Range(1, 3));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(3, 4));
      assertEquals("[#1-#4]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 4),
        new Range(1, 2));
      assertEquals("[#1-#4]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 10),
        new Range(2, 3));
      assertEquals("[#1-#a]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(2, 3),
        new Range(1, 10));
      assertEquals("[#1-#a]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('p', 't'));
      assertEquals("['a'-'z']", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('p', 't'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('a', 'z'));
      assertEquals("['a'-'z']", set.toString());
    }
  }

  @Test
  public void testMinusLetters() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .minus(RangeSet.of(new Range('k'), new Range('p', 'r')));
      assertEquals("['a'-'j'; 'l'-'o'; 's'-'z']", set.toString());
    }
  }

  @Test
  public void testComplementLetters() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .complement();
      RangeSet complement = RangeSet.builder().add(0x0, '`').add('{', 0x377).add(0x37a, 0x37f).add(0x384, 0x38a)
          .add(0x38c).add(0x38e, 0x3a1).add(0x3a3, 0x52f).add(0x531, 0x556).add(0x559, 0x55f).add(0x561, 0x587)
          .add(0x589, 0x58a).add(0x58d, 0x58f).add(0x591, 0x5c7).add(0x5d0, 0x5ea).add(0x5f0, 0x5f4).add(0x600, 0x61c)
          .add(0x61e, 0x70d).add(0x70f, 0x74a).add(0x74d, 0x7b1).add(0x7c0, 0x7fa).add(0x800, 0x82d).add(0x830, 0x83e)
          .add(0x840, 0x85b).add(0x85e).add(0x860, 0x86a).add(0x8a0, 0x8b4).add(0x8b6, 0x8bd).add(0x8d4, 0x983)
          .add(0x985, 0x98c).add(0x98f, 0x990).add(0x993, 0x9a8).add(0x9aa, 0x9b0).add(0x9b2).add(0x9b6, 0x9b9)
          .add(0x9bc, 0x9c4).add(0x9c7, 0x9c8).add(0x9cb, 0x9ce).add(0x9d7).add(0x9dc, 0x9dd).add(0x9df, 0x9e3)
          .add(0x9e6, 0x9fd).add(0xa01, 0xa03).add(0xa05, 0xa0a).add(0xa0f, 0xa10).add(0xa13, 0xa28).add(0xa2a, 0xa30)
          .add(0xa32, 0xa33).add(0xa35, 0xa36).add(0xa38, 0xa39).add(0xa3c).add(0xa3e, 0xa42).add(0xa47, 0xa48)
          .add(0xa4b, 0xa4d).add(0xa51).add(0xa59, 0xa5c).add(0xa5e).add(0xa66, 0xa75).add(0xa81, 0xa83)
          .add(0xa85, 0xa8d).add(0xa8f, 0xa91).add(0xa93, 0xaa8).add(0xaaa, 0xab0).add(0xab2, 0xab3).add(0xab5, 0xab9)
          .add(0xabc, 0xac5).add(0xac7, 0xac9).add(0xacb, 0xacd).add(0xad0).add(0xae0, 0xae3).add(0xae6, 0xaf1)
          .add(0xaf9, 0xaff).add(0xb01, 0xb03).add(0xb05, 0xb0c).add(0xb0f, 0xb10).add(0xb13, 0xb28).add(0xb2a, 0xb30)
          .add(0xb32, 0xb33).add(0xb35, 0xb39).add(0xb3c, 0xb44).add(0xb47, 0xb48).add(0xb4b, 0xb4d).add(0xb56, 0xb57)
          .add(0xb5c, 0xb5d).add(0xb5f, 0xb63).add(0xb66, 0xb77).add(0xb82, 0xb83).add(0xb85, 0xb8a).add(0xb8e, 0xb90)
          .add(0xb92, 0xb95).add(0xb99, 0xb9a).add(0xb9c).add(0xb9e, 0xb9f).add(0xba3, 0xba4).add(0xba8, 0xbaa)
          .add(0xbae, 0xbb9).add(0xbbe, 0xbc2).add(0xbc6, 0xbc8).add(0xbca, 0xbcd).add(0xbd0).add(0xbd7)
          .add(0xbe6, 0xbfa).add(0xc00, 0xc03).add(0xc05, 0xc0c).add(0xc0e, 0xc10).add(0xc12, 0xc28).add(0xc2a, 0xc39)
          .add(0xc3d, 0xc44).add(0xc46, 0xc48).add(0xc4a, 0xc4d).add(0xc55, 0xc56).add(0xc58, 0xc5a).add(0xc60, 0xc63)
          .add(0xc66, 0xc6f).add(0xc78, 0xc83).add(0xc85, 0xc8c).add(0xc8e, 0xc90).add(0xc92, 0xca8).add(0xcaa, 0xcb3)
          .add(0xcb5, 0xcb9).add(0xcbc, 0xcc4).add(0xcc6, 0xcc8).add(0xcca, 0xccd).add(0xcd5, 0xcd6).add(0xcde)
          .add(0xce0, 0xce3).add(0xce6, 0xcef).add(0xcf1, 0xcf2).add(0xd00, 0xd03).add(0xd05, 0xd0c).add(0xd0e, 0xd10)
          .add(0xd12, 0xd44).add(0xd46, 0xd48).add(0xd4a, 0xd4f).add(0xd54, 0xd63).add(0xd66, 0xd7f).add(0xd82, 0xd83)
          .add(0xd85, 0xd96).add(0xd9a, 0xdb1).add(0xdb3, 0xdbb).add(0xdbd).add(0xdc0, 0xdc6).add(0xdca)
          .add(0xdcf, 0xdd4).add(0xdd6).add(0xdd8, 0xddf).add(0xde6, 0xdef).add(0xdf2, 0xdf4).add(0xe01, 0xe3a)
          .add(0xe3f, 0xe5b).add(0xe81, 0xe82).add(0xe84).add(0xe87, 0xe88).add(0xe8a).add(0xe8d).add(0xe94, 0xe97)
          .add(0xe99, 0xe9f).add(0xea1, 0xea3).add(0xea5).add(0xea7).add(0xeaa, 0xeab).add(0xead, 0xeb9)
          .add(0xebb, 0xebd).add(0xec0, 0xec4).add(0xec6).add(0xec8, 0xecd).add(0xed0, 0xed9).add(0xedc, 0xedf)
          .add(0xf00, 0xf47).add(0xf49, 0xf6c).add(0xf71, 0xf97).add(0xf99, 0xfbc).add(0xfbe, 0xfcc).add(0xfce, 0xfda)
          .add(0x1000, 0x10c5).add(0x10c7).add(0x10cd).add(0x10d0, 0x1248).add(0x124a, 0x124d).add(0x1250, 0x1256)
          .add(0x1258).add(0x125a, 0x125d).add(0x1260, 0x1288).add(0x128a, 0x128d).add(0x1290, 0x12b0)
          .add(0x12b2, 0x12b5).add(0x12b8, 0x12be).add(0x12c0).add(0x12c2, 0x12c5).add(0x12c8, 0x12d6)
          .add(0x12d8, 0x1310).add(0x1312, 0x1315).add(0x1318, 0x135a).add(0x135d, 0x137c).add(0x1380, 0x1399)
          .add(0x13a0, 0x13f5).add(0x13f8, 0x13fd).add(0x1400, 0x169c).add(0x16a0, 0x16f8).add(0x1700, 0x170c)
          .add(0x170e, 0x1714).add(0x1720, 0x1736).add(0x1740, 0x1753).add(0x1760, 0x176c).add(0x176e, 0x1770)
          .add(0x1772, 0x1773).add(0x1780, 0x17dd).add(0x17e0, 0x17e9).add(0x17f0, 0x17f9).add(0x1800, 0x180e)
          .add(0x1810, 0x1819).add(0x1820, 0x1877).add(0x1880, 0x18aa).add(0x18b0, 0x18f5).add(0x1900, 0x191e)
          .add(0x1920, 0x192b).add(0x1930, 0x193b).add(0x1940).add(0x1944, 0x196d).add(0x1970, 0x1974)
          .add(0x1980, 0x19ab).add(0x19b0, 0x19c9).add(0x19d0, 0x19da).add(0x19de, 0x1a1b).add(0x1a1e, 0x1a5e)
          .add(0x1a60, 0x1a7c).add(0x1a7f, 0x1a89).add(0x1a90, 0x1a99).add(0x1aa0, 0x1aad).add(0x1ab0, 0x1abe)
          .add(0x1b00, 0x1b4b).add(0x1b50, 0x1b7c).add(0x1b80, 0x1bf3).add(0x1bfc, 0x1c37).add(0x1c3b, 0x1c49)
          .add(0x1c4d, 0x1c88).add(0x1cc0, 0x1cc7).add(0x1cd0, 0x1cf9).add(0x1d00, 0x1df9).add(0x1dfb, 0x1f15)
          .add(0x1f18, 0x1f1d).add(0x1f20, 0x1f45).add(0x1f48, 0x1f4d).add(0x1f50, 0x1f57).add(0x1f59).add(0x1f5b)
          .add(0x1f5d).add(0x1f5f, 0x1f7d).add(0x1f80, 0x1fb4).add(0x1fb6, 0x1fc4).add(0x1fc6, 0x1fd3)
          .add(0x1fd6, 0x1fdb).add(0x1fdd, 0x1fef).add(0x1ff2, 0x1ff4).add(0x1ff6, 0x1ffe).add(0x2000, 0x2064)
          .add(0x2066, 0x2071).add(0x2074, 0x208e).add(0x2090, 0x209c).add(0x20a0, 0x20bf).add(0x20d0, 0x20f0)
          .add(0x2100, 0x218b).add(0x2190, 0x2426).add(0x2440, 0x244a).add(0x2460, 0x2b73).add(0x2b76, 0x2b95)
          .add(0x2b98, 0x2bb9).add(0x2bbd, 0x2bc8).add(0x2bca, 0x2bd2).add(0x2bec, 0x2bef).add(0x2c00, 0x2c2e)
          .add(0x2c30, 0x2c5e).add(0x2c60, 0x2cf3).add(0x2cf9, 0x2d25).add(0x2d27).add(0x2d2d).add(0x2d30, 0x2d67)
          .add(0x2d6f, 0x2d70).add(0x2d7f, 0x2d96).add(0x2da0, 0x2da6).add(0x2da8, 0x2dae).add(0x2db0, 0x2db6)
          .add(0x2db8, 0x2dbe).add(0x2dc0, 0x2dc6).add(0x2dc8, 0x2dce).add(0x2dd0, 0x2dd6).add(0x2dd8, 0x2dde)
          .add(0x2de0, 0x2e49).add(0x2e80, 0x2e99).add(0x2e9b, 0x2ef3).add(0x2f00, 0x2fd5).add(0x2ff0, 0x2ffb)
          .add(0x3000, 0x303f).add(0x3041, 0x3096).add(0x3099, 0x30ff).add(0x3105, 0x312e).add(0x3131, 0x318e)
          .add(0x3190, 0x31ba).add(0x31c0, 0x31e3).add(0x31f0, 0x321e).add(0x3220, 0x4db5).add(0x4dc0, 0x9fea)
          .add(0xa000, 0xa48c).add(0xa490, 0xa4c6).add(0xa4d0, 0xa62b).add(0xa640, 0xa6f7).add(0xa700, 0xa7ae)
          .add(0xa7b0, 0xa7b7).add(0xa7f7, 0xa82b).add(0xa830, 0xa839).add(0xa840, 0xa877).add(0xa880, 0xa8c5)
          .add(0xa8ce, 0xa8d9).add(0xa8e0, 0xa8fd).add(0xa900, 0xa953).add(0xa95f, 0xa97c).add(0xa980, 0xa9cd)
          .add(0xa9cf, 0xa9d9).add(0xa9de, 0xa9fe).add(0xaa00, 0xaa36).add(0xaa40, 0xaa4d).add(0xaa50, 0xaa59)
          .add(0xaa5c, 0xaac2).add(0xaadb, 0xaaf6).add(0xab01, 0xab06).add(0xab09, 0xab0e).add(0xab11, 0xab16)
          .add(0xab20, 0xab26).add(0xab28, 0xab2e).add(0xab30, 0xab65).add(0xab70, 0xabed).add(0xabf0, 0xabf9)
          .add(0xac00, 0xd7a3).add(0xd7b0, 0xd7c6).add(0xd7cb, 0xd7fb).add(0xe000, 0xfa6d).add(0xfa70, 0xfad9)
          .add(0xfb00, 0xfb06).add(0xfb13, 0xfb17).add(0xfb1d, 0xfb36).add(0xfb38, 0xfb3c).add(0xfb3e)
          .add(0xfb40, 0xfb41).add(0xfb43, 0xfb44).add(0xfb46, 0xfbc1).add(0xfbd3, 0xfd3f).add(0xfd50, 0xfd8f)
          .add(0xfd92, 0xfdc7).add(0xfdf0, 0xfdfd).add(0xfe00, 0xfe19).add(0xfe20, 0xfe52).add(0xfe54, 0xfe66)
          .add(0xfe68, 0xfe6b).add(0xfe70, 0xfe74).add(0xfe76, 0xfefc).add(0xfeff).add(0xff01, 0xffbe)
          .add(0xffc2, 0xffc7).add(0xffca, 0xffcf).add(0xffd2, 0xffd7).add(0xffda, 0xffdc).add(0xffe0, 0xffe6)
          .add(0xffe8, 0xffee).add(0xfff9, 0xfffd).add(0x10000, 0x1000b).add(0x1000d, 0x10026).add(0x10028, 0x1003a)
          .add(0x1003c, 0x1003d).add(0x1003f, 0x1004d).add(0x10050, 0x1005d).add(0x10080, 0x100fa).add(0x10100, 0x10102)
          .add(0x10107, 0x10133).add(0x10137, 0x1018e).add(0x10190, 0x1019b).add(0x101a0).add(0x101d0, 0x101fd)
          .add(0x10280, 0x1029c).add(0x102a0, 0x102d0).add(0x102e0, 0x102fb).add(0x10300, 0x10323).add(0x1032d, 0x1034a)
          .add(0x10350, 0x1037a).add(0x10380, 0x1039d).add(0x1039f, 0x103c3).add(0x103c8, 0x103d5).add(0x10400, 0x1049d)
          .add(0x104a0, 0x104a9).add(0x104b0, 0x104d3).add(0x104d8, 0x104fb).add(0x10500, 0x10527).add(0x10530, 0x10563)
          .add(0x1056f).add(0x10600, 0x10736).add(0x10740, 0x10755).add(0x10760, 0x10767).add(0x10800, 0x10805)
          .add(0x10808).add(0x1080a, 0x10835).add(0x10837, 0x10838).add(0x1083c).add(0x1083f, 0x10855)
          .add(0x10857, 0x1089e).add(0x108a7, 0x108af).add(0x108e0, 0x108f2).add(0x108f4, 0x108f5).add(0x108fb, 0x1091b)
          .add(0x1091f, 0x10939).add(0x1093f).add(0x10980, 0x109b7).add(0x109bc, 0x109cf).add(0x109d2, 0x10a03)
          .add(0x10a05, 0x10a06).add(0x10a0c, 0x10a13).add(0x10a15, 0x10a17).add(0x10a19, 0x10a33).add(0x10a38, 0x10a3a)
          .add(0x10a3f, 0x10a47).add(0x10a50, 0x10a58).add(0x10a60, 0x10a9f).add(0x10ac0, 0x10ae6).add(0x10aeb, 0x10af6)
          .add(0x10b00, 0x10b35).add(0x10b39, 0x10b55).add(0x10b58, 0x10b72).add(0x10b78, 0x10b91).add(0x10b99, 0x10b9c)
          .add(0x10ba9, 0x10baf).add(0x10c00, 0x10c48).add(0x10c80, 0x10cb2).add(0x10cc0, 0x10cf2).add(0x10cfa, 0x10cff)
          .add(0x10e60, 0x10e7e).add(0x11000, 0x1104d).add(0x11052, 0x1106f).add(0x1107f, 0x110c1).add(0x110d0, 0x110e8)
          .add(0x110f0, 0x110f9).add(0x11100, 0x11134).add(0x11136, 0x11143).add(0x11150, 0x11176).add(0x11180, 0x111cd)
          .add(0x111d0, 0x111df).add(0x111e1, 0x111f4).add(0x11200, 0x11211).add(0x11213, 0x1123e).add(0x11280, 0x11286)
          .add(0x11288).add(0x1128a, 0x1128d).add(0x1128f, 0x1129d).add(0x1129f, 0x112a9).add(0x112b0, 0x112ea)
          .add(0x112f0, 0x112f9).add(0x11300, 0x11303).add(0x11305, 0x1130c).add(0x1130f, 0x11310).add(0x11313, 0x11328)
          .add(0x1132a, 0x11330).add(0x11332, 0x11333).add(0x11335, 0x11339).add(0x1133c, 0x11344).add(0x11347, 0x11348)
          .add(0x1134b, 0x1134d).add(0x11350).add(0x11357).add(0x1135d, 0x11363).add(0x11366, 0x1136c)
          .add(0x11370, 0x11374).add(0x11400, 0x11459).add(0x1145b).add(0x1145d).add(0x11480, 0x114c7)
          .add(0x114d0, 0x114d9).add(0x11580, 0x115b5).add(0x115b8, 0x115dd).add(0x11600, 0x11644).add(0x11650, 0x11659)
          .add(0x11660, 0x1166c).add(0x11680, 0x116b7).add(0x116c0, 0x116c9).add(0x11700, 0x11719).add(0x1171d, 0x1172b)
          .add(0x11730, 0x1173f).add(0x118a0, 0x118f2).add(0x118ff).add(0x11a00, 0x11a47).add(0x11a50, 0x11a83)
          .add(0x11a86, 0x11a9c).add(0x11a9e, 0x11aa2).add(0x11ac0, 0x11af8).add(0x11c00, 0x11c08).add(0x11c0a, 0x11c36)
          .add(0x11c38, 0x11c45).add(0x11c50, 0x11c6c).add(0x11c70, 0x11c8f).add(0x11c92, 0x11ca7).add(0x11ca9, 0x11cb6)
          .add(0x11d00, 0x11d06).add(0x11d08, 0x11d09).add(0x11d0b, 0x11d36).add(0x11d3a).add(0x11d3c, 0x11d3d)
          .add(0x11d3f, 0x11d47).add(0x11d50, 0x11d59).add(0x12000, 0x12399).add(0x12400, 0x1246e).add(0x12470, 0x12474)
          .add(0x12480, 0x12543).add(0x13000, 0x1342e).add(0x14400, 0x14646).add(0x16800, 0x16a38).add(0x16a40, 0x16a5e)
          .add(0x16a60, 0x16a69).add(0x16a6e, 0x16a6f).add(0x16ad0, 0x16aed).add(0x16af0, 0x16af5).add(0x16b00, 0x16b45)
          .add(0x16b50, 0x16b59).add(0x16b5b, 0x16b61).add(0x16b63, 0x16b77).add(0x16b7d, 0x16b8f).add(0x16f00, 0x16f44)
          .add(0x16f50, 0x16f7e).add(0x16f8f, 0x16f9f).add(0x16fe0, 0x16fe1).add(0x17000, 0x187ec).add(0x18800, 0x18af2)
          .add(0x1b000, 0x1b11e).add(0x1b170, 0x1b2fb).add(0x1bc00, 0x1bc6a).add(0x1bc70, 0x1bc7c).add(0x1bc80, 0x1bc88)
          .add(0x1bc90, 0x1bc99).add(0x1bc9c, 0x1bca3).add(0x1d000, 0x1d0f5).add(0x1d100, 0x1d126).add(0x1d129, 0x1d1e8)
          .add(0x1d200, 0x1d245).add(0x1d300, 0x1d356).add(0x1d360, 0x1d371).add(0x1d400, 0x1d454).add(0x1d456, 0x1d49c)
          .add(0x1d49e, 0x1d49f).add(0x1d4a2).add(0x1d4a5, 0x1d4a6).add(0x1d4a9, 0x1d4ac).add(0x1d4ae, 0x1d4b9)
          .add(0x1d4bb).add(0x1d4bd, 0x1d4c3).add(0x1d4c5, 0x1d505).add(0x1d507, 0x1d50a).add(0x1d50d, 0x1d514)
          .add(0x1d516, 0x1d51c).add(0x1d51e, 0x1d539).add(0x1d53b, 0x1d53e).add(0x1d540, 0x1d544).add(0x1d546)
          .add(0x1d54a, 0x1d550).add(0x1d552, 0x1d6a5).add(0x1d6a8, 0x1d7cb).add(0x1d7ce, 0x1da8b).add(0x1da9b, 0x1da9f)
          .add(0x1daa1, 0x1daaf).add(0x1e000, 0x1e006).add(0x1e008, 0x1e018).add(0x1e01b, 0x1e021).add(0x1e023, 0x1e024)
          .add(0x1e026, 0x1e02a).add(0x1e800, 0x1e8c4).add(0x1e8c7, 0x1e8d6).add(0x1e900, 0x1e94a).add(0x1e950, 0x1e959)
          .add(0x1e95e, 0x1e95f).add(0x1ee00, 0x1ee03).add(0x1ee05, 0x1ee1f).add(0x1ee21, 0x1ee22).add(0x1ee24)
          .add(0x1ee27).add(0x1ee29, 0x1ee32).add(0x1ee34, 0x1ee37).add(0x1ee39).add(0x1ee3b).add(0x1ee42).add(0x1ee47)
          .add(0x1ee49).add(0x1ee4b).add(0x1ee4d, 0x1ee4f).add(0x1ee51, 0x1ee52).add(0x1ee54).add(0x1ee57).add(0x1ee59)
          .add(0x1ee5b).add(0x1ee5d).add(0x1ee5f).add(0x1ee61, 0x1ee62).add(0x1ee64).add(0x1ee67, 0x1ee6a)
          .add(0x1ee6c, 0x1ee72).add(0x1ee74, 0x1ee77).add(0x1ee79, 0x1ee7c).add(0x1ee7e).add(0x1ee80, 0x1ee89)
          .add(0x1ee8b, 0x1ee9b).add(0x1eea1, 0x1eea3).add(0x1eea5, 0x1eea9).add(0x1eeab, 0x1eebb).add(0x1eef0, 0x1eef1)
          .add(0x1f000, 0x1f02b).add(0x1f030, 0x1f093).add(0x1f0a0, 0x1f0ae).add(0x1f0b1, 0x1f0bf).add(0x1f0c1, 0x1f0cf)
          .add(0x1f0d1, 0x1f0f5).add(0x1f100, 0x1f10c).add(0x1f110, 0x1f12e).add(0x1f130, 0x1f16b).add(0x1f170, 0x1f1ac)
          .add(0x1f1e6, 0x1f202).add(0x1f210, 0x1f23b).add(0x1f240, 0x1f248).add(0x1f250, 0x1f251).add(0x1f260, 0x1f265)
          .add(0x1f300, 0x1f6d4).add(0x1f6e0, 0x1f6ec).add(0x1f6f0, 0x1f6f8).add(0x1f700, 0x1f773).add(0x1f780, 0x1f7d4)
          .add(0x1f800, 0x1f80b).add(0x1f810, 0x1f847).add(0x1f850, 0x1f859).add(0x1f860, 0x1f887).add(0x1f890, 0x1f8ad)
          .add(0x1f900, 0x1f90b).add(0x1f910, 0x1f93e).add(0x1f940, 0x1f94c).add(0x1f950, 0x1f96b).add(0x1f980, 0x1f997)
          .add(0x1f9c0).add(0x1f9d0, 0x1f9e6).add(0x20000, 0x2a6d6).add(0x2a700, 0x2b734).add(0x2b740, 0x2b81d)
          .add(0x2b820, 0x2cea1).add(0x2ceb0, 0x2ebe0).add(0x2f800, 0x2fa1d).add(0xe0001).add(0xe0020, 0xe007f)
          .add(0xe0100, 0xe01ef).add(0xf0000, 0xffffd).add(0x100000, 0x10fffd).build();
      assertEquals(complement, set);
      assertEquals("['a'-'z']", set.complement().toString());
    }
  }

  @Test
  public void testRandomInsertions() {
    Random random = new Random();
    long seed = random.nextLong();
    random.setSeed(seed);
    String msgPrefix = "With seed " + seed + "L: ";
    for (int iteration = 0; iteration < 128; ++iteration) {
      int numberOfRanges = 1 + random.nextInt(16); // .nextInt(100);
      List<Range> ranges = new ArrayList<>();
      for (int r = 0; r < numberOfRanges; ++r) {
        int f = ' ' + random.nextInt(96);
        int l = f + random.nextInt(128 - f);
        Range range = new Range(f, l);
        ranges.add(range);
      }
      String string = null;
      String complementString = null;
      for (int permutation = 0; permutation < 64; ++permutation) {
        RangeSet.Builder builder = RangeSet.builder();
        Collections.shuffle(ranges);
        for (Range range : ranges)
          builder.add(range);
        RangeSet set = builder.build();
        RangeSet complement = set.complement();
        if (string == null) {
          string = set.toString();
          complementString = complement.toString();
        }
        else {
          assertEquals(string, set.toString(), msgPrefix + "unexpected result");
          assertEquals(complementString, complement.toString(), msgPrefix + "unexpected result");
        }
      }
    }
  }

  @Test
  public void testOfUnicodeCharClass() {
    RangeSet.unicodeClasses.forEach((k, v) -> assertEquals(v, RangeSet.computeFromPattern(k)));

    String stringOfZs = "[' '; #a0; #1680; #2000-#200a; #202f; #205f; #3000]";
    String stringOfL  = "['A'-'Z'; 'a'-'z'; #aa; #b5; #ba; #c0-#d6; #d8-#f6; #f8-#2c1; #2c6-#2d1; #2e0-#2e4; #2ec; #2ee; #370-#374; #376-#377; #37a-#37d; #37f; #386; #388-#38a; #38c; #38e-#3a1; #3a3-#3f5; #3f7-#481; #48a-#52f; #531-#556; #559; #561-#587; #5d0-#5ea; #5f0-#5f2; #620-#64a; #66e-#66f; #671-#6d3; #6d5; #6e5-#6e6; #6ee-#6ef; #6fa-#6fc; #6ff; #710; #712-#72f; #74d-#7a5; #7b1; #7ca-#7ea; #7f4-#7f5; #7fa; #800-#815; #81a; #824; #828; #840-#858; #860-#86a; #8a0-#8b4; #8b6-#8bd; #904-#939; #93d; #950; #958-#961; #971-#980; #985-#98c; #98f-#990; #993-#9a8; #9aa-#9b0; #9b2; #9b6-#9b9; #9bd; #9ce; #9dc-#9dd; #9df-#9e1; #9f0-#9f1; #9fc; #a05-#a0a; #a0f-#a10; #a13-#a28; #a2a-#a30; #a32-#a33; #a35-#a36; #a38-#a39; #a59-#a5c; #a5e; #a72-#a74; #a85-#a8d; #a8f-#a91; #a93-#aa8; #aaa-#ab0; #ab2-#ab3; #ab5-#ab9; #abd; #ad0; #ae0-#ae1; #af9; #b05-#b0c; #b0f-#b10; #b13-#b28; #b2a-#b30; #b32-#b33; #b35-#b39; #b3d; #b5c-#b5d; #b5f-#b61; #b71; #b83; #b85-#b8a; #b8e-#b90; #b92-#b95; #b99-#b9a; #b9c; #b9e-#b9f; #ba3-#ba4; #ba8-#baa; #bae-#bb9; #bd0; #c05-#c0c; #c0e-#c10; #c12-#c28; #c2a-#c39; #c3d; #c58-#c5a; #c60-#c61; #c80; #c85-#c8c; #c8e-#c90; #c92-#ca8; #caa-#cb3; #cb5-#cb9; #cbd; #cde; #ce0-#ce1; #cf1-#cf2; #d05-#d0c; #d0e-#d10; #d12-#d3a; #d3d; #d4e; #d54-#d56; #d5f-#d61; #d7a-#d7f; #d85-#d96; #d9a-#db1; #db3-#dbb; #dbd; #dc0-#dc6; #e01-#e30; #e32-#e33; #e40-#e46; #e81-#e82; #e84; #e87-#e88; #e8a; #e8d; #e94-#e97; #e99-#e9f; #ea1-#ea3; #ea5; #ea7; #eaa-#eab; #ead-#eb0; #eb2-#eb3; #ebd; #ec0-#ec4; #ec6; #edc-#edf; #f00; #f40-#f47; #f49-#f6c; #f88-#f8c; #1000-#102a; #103f; #1050-#1055; #105a-#105d; #1061; #1065-#1066; #106e-#1070; #1075-#1081; #108e; #10a0-#10c5; #10c7; #10cd; #10d0-#10fa; #10fc-#1248; #124a-#124d; #1250-#1256; #1258; #125a-#125d; #1260-#1288; #128a-#128d; #1290-#12b0; #12b2-#12b5; #12b8-#12be; #12c0; #12c2-#12c5; #12c8-#12d6; #12d8-#1310; #1312-#1315; #1318-#135a; #1380-#138f; #13a0-#13f5; #13f8-#13fd; #1401-#166c; #166f-#167f; #1681-#169a; #16a0-#16ea; #16f1-#16f8; #1700-#170c; #170e-#1711; #1720-#1731; #1740-#1751; #1760-#176c; #176e-#1770; #1780-#17b3; #17d7; #17dc; #1820-#1877; #1880-#1884; #1887-#18a8; #18aa; #18b0-#18f5; #1900-#191e; #1950-#196d; #1970-#1974; #1980-#19ab; #19b0-#19c9; #1a00-#1a16; #1a20-#1a54; #1aa7; #1b05-#1b33; #1b45-#1b4b; #1b83-#1ba0; #1bae-#1baf; #1bba-#1be5; #1c00-#1c23; #1c4d-#1c4f; #1c5a-#1c7d; #1c80-#1c88; #1ce9-#1cec; #1cee-#1cf1; #1cf5-#1cf6; #1d00-#1dbf; #1e00-#1f15; #1f18-#1f1d; #1f20-#1f45; #1f48-#1f4d; #1f50-#1f57; #1f59; #1f5b; #1f5d; #1f5f-#1f7d; #1f80-#1fb4; #1fb6-#1fbc; #1fbe; #1fc2-#1fc4; #1fc6-#1fcc; #1fd0-#1fd3; #1fd6-#1fdb; #1fe0-#1fec; #1ff2-#1ff4; #1ff6-#1ffc; #2071; #207f; #2090-#209c; #2102; #2107; #210a-#2113; #2115; #2119-#211d; #2124; #2126; #2128; #212a-#212d; #212f-#2139; #213c-#213f; #2145-#2149; #214e; #2183-#2184; #2c00-#2c2e; #2c30-#2c5e; #2c60-#2ce4; #2ceb-#2cee; #2cf2-#2cf3; #2d00-#2d25; #2d27; #2d2d; #2d30-#2d67; #2d6f; #2d80-#2d96; #2da0-#2da6; #2da8-#2dae; #2db0-#2db6; #2db8-#2dbe; #2dc0-#2dc6; #2dc8-#2dce; #2dd0-#2dd6; #2dd8-#2dde; #2e2f; #3005-#3006; #3031-#3035; #303b-#303c; #3041-#3096; #309d-#309f; #30a1-#30fa; #30fc-#30ff; #3105-#312e; #3131-#318e; #31a0-#31ba; #31f0-#31ff; #3400-#4db5; #4e00-#9fea; #a000-#a48c; #a4d0-#a4fd; #a500-#a60c; #a610-#a61f; #a62a-#a62b; #a640-#a66e; #a67f-#a69d; #a6a0-#a6e5; #a717-#a71f; #a722-#a788; #a78b-#a7ae; #a7b0-#a7b7; #a7f7-#a801; #a803-#a805; #a807-#a80a; #a80c-#a822; #a840-#a873; #a882-#a8b3; #a8f2-#a8f7; #a8fb; #a8fd; #a90a-#a925; #a930-#a946; #a960-#a97c; #a984-#a9b2; #a9cf; #a9e0-#a9e4; #a9e6-#a9ef; #a9fa-#a9fe; #aa00-#aa28; #aa40-#aa42; #aa44-#aa4b; #aa60-#aa76; #aa7a; #aa7e-#aaaf; #aab1; #aab5-#aab6; #aab9-#aabd; #aac0; #aac2; #aadb-#aadd; #aae0-#aaea; #aaf2-#aaf4; #ab01-#ab06; #ab09-#ab0e; #ab11-#ab16; #ab20-#ab26; #ab28-#ab2e; #ab30-#ab5a; #ab5c-#ab65; #ab70-#abe2; #ac00-#d7a3; #d7b0-#d7c6; #d7cb-#d7fb; #f900-#fa6d; #fa70-#fad9; #fb00-#fb06; #fb13-#fb17; #fb1d; #fb1f-#fb28; #fb2a-#fb36; #fb38-#fb3c; #fb3e; #fb40-#fb41; #fb43-#fb44; #fb46-#fbb1; #fbd3-#fd3d; #fd50-#fd8f; #fd92-#fdc7; #fdf0-#fdfb; #fe70-#fe74; #fe76-#fefc; #ff21-#ff3a; #ff41-#ff5a; #ff66-#ffbe; #ffc2-#ffc7; #ffca-#ffcf; #ffd2-#ffd7; #ffda-#ffdc; #10000-#1000b; #1000d-#10026; #10028-#1003a; #1003c-#1003d; #1003f-#1004d; #10050-#1005d; #10080-#100fa; #10280-#1029c; #102a0-#102d0; #10300-#1031f; #1032d-#10340; #10342-#10349; #10350-#10375; #10380-#1039d; #103a0-#103c3; #103c8-#103cf; #10400-#1049d; #104b0-#104d3; #104d8-#104fb; #10500-#10527; #10530-#10563; #10600-#10736; #10740-#10755; #10760-#10767; #10800-#10805; #10808; #1080a-#10835; #10837-#10838; #1083c; #1083f-#10855; #10860-#10876; #10880-#1089e; #108e0-#108f2; #108f4-#108f5; #10900-#10915; #10920-#10939; #10980-#109b7; #109be-#109bf; #10a00; #10a10-#10a13; #10a15-#10a17; #10a19-#10a33; #10a60-#10a7c; #10a80-#10a9c; #10ac0-#10ac7; #10ac9-#10ae4; #10b00-#10b35; #10b40-#10b55; #10b60-#10b72; #10b80-#10b91; #10c00-#10c48; #10c80-#10cb2; #10cc0-#10cf2; #11003-#11037; #11083-#110af; #110d0-#110e8; #11103-#11126; #11150-#11172; #11176; #11183-#111b2; #111c1-#111c4; #111da; #111dc; #11200-#11211; #11213-#1122b; #11280-#11286; #11288; #1128a-#1128d; #1128f-#1129d; #1129f-#112a8; #112b0-#112de; #11305-#1130c; #1130f-#11310; #11313-#11328; #1132a-#11330; #11332-#11333; #11335-#11339; #1133d; #11350; #1135d-#11361; #11400-#11434; #11447-#1144a; #11480-#114af; #114c4-#114c5; #114c7; #11580-#115ae; #115d8-#115db; #11600-#1162f; #11644; #11680-#116aa; #11700-#11719; #118a0-#118df; #118ff; #11a00; #11a0b-#11a32; #11a3a; #11a50; #11a5c-#11a83; #11a86-#11a89; #11ac0-#11af8; #11c00-#11c08; #11c0a-#11c2e; #11c40; #11c72-#11c8f; #11d00-#11d06; #11d08-#11d09; #11d0b-#11d30; #11d46; #12000-#12399; #12480-#12543; #13000-#1342e; #14400-#14646; #16800-#16a38; #16a40-#16a5e; #16ad0-#16aed; #16b00-#16b2f; #16b40-#16b43; #16b63-#16b77; #16b7d-#16b8f; #16f00-#16f44; #16f50; #16f93-#16f9f; #16fe0-#16fe1; #17000-#187ec; #18800-#18af2; #1b000-#1b11e; #1b170-#1b2fb; #1bc00-#1bc6a; #1bc70-#1bc7c; #1bc80-#1bc88; #1bc90-#1bc99; #1d400-#1d454; #1d456-#1d49c; #1d49e-#1d49f; #1d4a2; #1d4a5-#1d4a6; #1d4a9-#1d4ac; #1d4ae-#1d4b9; #1d4bb; #1d4bd-#1d4c3; #1d4c5-#1d505; #1d507-#1d50a; #1d50d-#1d514; #1d516-#1d51c; #1d51e-#1d539; #1d53b-#1d53e; #1d540-#1d544; #1d546; #1d54a-#1d550; #1d552-#1d6a5; #1d6a8-#1d6c0; #1d6c2-#1d6da; #1d6dc-#1d6fa; #1d6fc-#1d714; #1d716-#1d734; #1d736-#1d74e; #1d750-#1d76e; #1d770-#1d788; #1d78a-#1d7a8; #1d7aa-#1d7c2; #1d7c4-#1d7cb; #1e800-#1e8c4; #1e900-#1e943; #1ee00-#1ee03; #1ee05-#1ee1f; #1ee21-#1ee22; #1ee24; #1ee27; #1ee29-#1ee32; #1ee34-#1ee37; #1ee39; #1ee3b; #1ee42; #1ee47; #1ee49; #1ee4b; #1ee4d-#1ee4f; #1ee51-#1ee52; #1ee54; #1ee57; #1ee59; #1ee5b; #1ee5d; #1ee5f; #1ee61-#1ee62; #1ee64; #1ee67-#1ee6a; #1ee6c-#1ee72; #1ee74-#1ee77; #1ee79-#1ee7c; #1ee7e; #1ee80-#1ee89; #1ee8b-#1ee9b; #1eea1-#1eea3; #1eea5-#1eea9; #1eeab-#1eebb; #20000-#2a6d6; #2a700-#2b734; #2b740-#2b81d; #2b820-#2cea1; #2ceb0-#2ebe0; #2f800-#2fa1d]";
    String stringOfNd = "['0'-'9'; #660-#669; #6f0-#6f9; #7c0-#7c9; #966-#96f; #9e6-#9ef; #a66-#a6f; #ae6-#aef; #b66-#b6f; #be6-#bef; #c66-#c6f; #ce6-#cef; #d66-#d6f; #de6-#def; #e50-#e59; #ed0-#ed9; #f20-#f29; #1040-#1049; #1090-#1099; #17e0-#17e9; #1810-#1819; #1946-#194f; #19d0-#19d9; #1a80-#1a89; #1a90-#1a99; #1b50-#1b59; #1bb0-#1bb9; #1c40-#1c49; #1c50-#1c59; #a620-#a629; #a8d0-#a8d9; #a900-#a909; #a9d0-#a9d9; #a9f0-#a9f9; #aa50-#aa59; #abf0-#abf9; #ff10-#ff19; #104a0-#104a9; #11066-#1106f; #110f0-#110f9; #11136-#1113f; #111d0-#111d9; #112f0-#112f9; #11450-#11459; #114d0-#114d9; #11650-#11659; #116c0-#116c9; #11730-#11739; #118e0-#118e9; #11c50-#11c59; #11d50-#11d59; #16a60-#16a69; #16b50-#16b59; #1d7ce-#1d7ff; #1e950-#1e959]";
    String stringOfMn = "[#300-#36f; #483-#487; #591-#5bd; #5bf; #5c1-#5c2; #5c4-#5c5; #5c7; #610-#61a; #64b-#65f; #670; #6d6-#6dc; #6df-#6e4; #6e7-#6e8; #6ea-#6ed; #711; #730-#74a; #7a6-#7b0; #7eb-#7f3; #816-#819; #81b-#823; #825-#827; #829-#82d; #859-#85b; #8d4-#8e1; #8e3-#902; #93a; #93c; #941-#948; #94d; #951-#957; #962-#963; #981; #9bc; #9c1-#9c4; #9cd; #9e2-#9e3; #a01-#a02; #a3c; #a41-#a42; #a47-#a48; #a4b-#a4d; #a51; #a70-#a71; #a75; #a81-#a82; #abc; #ac1-#ac5; #ac7-#ac8; #acd; #ae2-#ae3; #afa-#aff; #b01; #b3c; #b3f; #b41-#b44; #b4d; #b56; #b62-#b63; #b82; #bc0; #bcd; #c00; #c3e-#c40; #c46-#c48; #c4a-#c4d; #c55-#c56; #c62-#c63; #c81; #cbc; #cbf; #cc6; #ccc-#ccd; #ce2-#ce3; #d00-#d01; #d3b-#d3c; #d41-#d44; #d4d; #d62-#d63; #dca; #dd2-#dd4; #dd6; #e31; #e34-#e3a; #e47-#e4e; #eb1; #eb4-#eb9; #ebb-#ebc; #ec8-#ecd; #f18-#f19; #f35; #f37; #f39; #f71-#f7e; #f80-#f84; #f86-#f87; #f8d-#f97; #f99-#fbc; #fc6; #102d-#1030; #1032-#1037; #1039-#103a; #103d-#103e; #1058-#1059; #105e-#1060; #1071-#1074; #1082; #1085-#1086; #108d; #109d; #135d-#135f; #1712-#1714; #1732-#1734; #1752-#1753; #1772-#1773; #17b4-#17b5; #17b7-#17bd; #17c6; #17c9-#17d3; #17dd; #180b-#180d; #1885-#1886; #18a9; #1920-#1922; #1927-#1928; #1932; #1939-#193b; #1a17-#1a18; #1a1b; #1a56; #1a58-#1a5e; #1a60; #1a62; #1a65-#1a6c; #1a73-#1a7c; #1a7f; #1ab0-#1abd; #1b00-#1b03; #1b34; #1b36-#1b3a; #1b3c; #1b42; #1b6b-#1b73; #1b80-#1b81; #1ba2-#1ba5; #1ba8-#1ba9; #1bab-#1bad; #1be6; #1be8-#1be9; #1bed; #1bef-#1bf1; #1c2c-#1c33; #1c36-#1c37; #1cd0-#1cd2; #1cd4-#1ce0; #1ce2-#1ce8; #1ced; #1cf4; #1cf8-#1cf9; #1dc0-#1df9; #1dfb-#1dff; #20d0-#20dc; #20e1; #20e5-#20f0; #2cef-#2cf1; #2d7f; #2de0-#2dff; #302a-#302d; #3099-#309a; #a66f; #a674-#a67d; #a69e-#a69f; #a6f0-#a6f1; #a802; #a806; #a80b; #a825-#a826; #a8c4-#a8c5; #a8e0-#a8f1; #a926-#a92d; #a947-#a951; #a980-#a982; #a9b3; #a9b6-#a9b9; #a9bc; #a9e5; #aa29-#aa2e; #aa31-#aa32; #aa35-#aa36; #aa43; #aa4c; #aa7c; #aab0; #aab2-#aab4; #aab7-#aab8; #aabe-#aabf; #aac1; #aaec-#aaed; #aaf6; #abe5; #abe8; #abed; #fb1e; #fe00-#fe0f; #fe20-#fe2f; #101fd; #102e0; #10376-#1037a; #10a01-#10a03; #10a05-#10a06; #10a0c-#10a0f; #10a38-#10a3a; #10a3f; #10ae5-#10ae6; #11001; #11038-#11046; #1107f-#11081; #110b3-#110b6; #110b9-#110ba; #11100-#11102; #11127-#1112b; #1112d-#11134; #11173; #11180-#11181; #111b6-#111be; #111ca-#111cc; #1122f-#11231; #11234; #11236-#11237; #1123e; #112df; #112e3-#112ea; #11300-#11301; #1133c; #11340; #11366-#1136c; #11370-#11374; #11438-#1143f; #11442-#11444; #11446; #114b3-#114b8; #114ba; #114bf-#114c0; #114c2-#114c3; #115b2-#115b5; #115bc-#115bd; #115bf-#115c0; #115dc-#115dd; #11633-#1163a; #1163d; #1163f-#11640; #116ab; #116ad; #116b0-#116b5; #116b7; #1171d-#1171f; #11722-#11725; #11727-#1172b; #11a01-#11a06; #11a09-#11a0a; #11a33-#11a38; #11a3b-#11a3e; #11a47; #11a51-#11a56; #11a59-#11a5b; #11a8a-#11a96; #11a98-#11a99; #11c30-#11c36; #11c38-#11c3d; #11c3f; #11c92-#11ca7; #11caa-#11cb0; #11cb2-#11cb3; #11cb5-#11cb6; #11d31-#11d36; #11d3a; #11d3c-#11d3d; #11d3f-#11d45; #11d47; #16af0-#16af4; #16b30-#16b36; #16f8f-#16f92; #1bc9d-#1bc9e; #1d167-#1d169; #1d17b-#1d182; #1d185-#1d18b; #1d1aa-#1d1ad; #1d242-#1d244; #1da00-#1da36; #1da3b-#1da6c; #1da75; #1da84; #1da9b-#1da9f; #1daa1-#1daaf; #1e000-#1e006; #1e008-#1e018; #1e01b-#1e021; #1e023-#1e024; #1e026-#1e02a; #1e8d0-#1e8d6; #1e944-#1e94a; #e0100-#e01ef]";

    RangeSet setOfZs = RangeSet.of("Zs");
    RangeSet setOfL  = RangeSet.of("L");
    RangeSet setOfNd = RangeSet.of("Nd");
    RangeSet setOfMn = RangeSet.of("Mn");

    assertEquals(stringOfZs, setOfZs.toString());
    assertEquals(stringOfL,  setOfL.toString());
    assertEquals(stringOfNd, setOfNd.toString());
    assertEquals(stringOfMn, setOfMn.toString());

    assertEquals(setOfZs, RangeSet.computeFromPattern("Zs"));
    assertEquals(setOfL,  RangeSet.computeFromPattern("L"));
    assertEquals(setOfNd, RangeSet.computeFromPattern("Nd"));
    assertEquals(setOfMn, RangeSet.computeFromPattern("Mn"));
  }

  @Test
  public void testAlphabet() {
    String stringOfAlphabet = "[#0-#377; #37a-#37f; #384-#38a; #38c; #38e-#3a1; #3a3-#52f; #531-#556; #559-#55f; #561-#587; #589-#58a; #58d-#58f; #591-#5c7; #5d0-#5ea; #5f0-#5f4; #600-#61c; #61e-#70d; #70f-#74a; #74d-#7b1; #7c0-#7fa; #800-#82d; #830-#83e; #840-#85b; #85e; #860-#86a; #8a0-#8b4; #8b6-#8bd; #8d4-#983; #985-#98c; #98f-#990; #993-#9a8; #9aa-#9b0; #9b2; #9b6-#9b9; #9bc-#9c4; #9c7-#9c8; #9cb-#9ce; #9d7; #9dc-#9dd; #9df-#9e3; #9e6-#9fd; #a01-#a03; #a05-#a0a; #a0f-#a10; #a13-#a28; #a2a-#a30; #a32-#a33; #a35-#a36; #a38-#a39; #a3c; #a3e-#a42; #a47-#a48; #a4b-#a4d; #a51; #a59-#a5c; #a5e; #a66-#a75; #a81-#a83; #a85-#a8d; #a8f-#a91; #a93-#aa8; #aaa-#ab0; #ab2-#ab3; #ab5-#ab9; #abc-#ac5; #ac7-#ac9; #acb-#acd; #ad0; #ae0-#ae3; #ae6-#af1; #af9-#aff; #b01-#b03; #b05-#b0c; #b0f-#b10; #b13-#b28; #b2a-#b30; #b32-#b33; #b35-#b39; #b3c-#b44; #b47-#b48; #b4b-#b4d; #b56-#b57; #b5c-#b5d; #b5f-#b63; #b66-#b77; #b82-#b83; #b85-#b8a; #b8e-#b90; #b92-#b95; #b99-#b9a; #b9c; #b9e-#b9f; #ba3-#ba4; #ba8-#baa; #bae-#bb9; #bbe-#bc2; #bc6-#bc8; #bca-#bcd; #bd0; #bd7; #be6-#bfa; #c00-#c03; #c05-#c0c; #c0e-#c10; #c12-#c28; #c2a-#c39; #c3d-#c44; #c46-#c48; #c4a-#c4d; #c55-#c56; #c58-#c5a; #c60-#c63; #c66-#c6f; #c78-#c83; #c85-#c8c; #c8e-#c90; #c92-#ca8; #caa-#cb3; #cb5-#cb9; #cbc-#cc4; #cc6-#cc8; #cca-#ccd; #cd5-#cd6; #cde; #ce0-#ce3; #ce6-#cef; #cf1-#cf2; #d00-#d03; #d05-#d0c; #d0e-#d10; #d12-#d44; #d46-#d48; #d4a-#d4f; #d54-#d63; #d66-#d7f; #d82-#d83; #d85-#d96; #d9a-#db1; #db3-#dbb; #dbd; #dc0-#dc6; #dca; #dcf-#dd4; #dd6; #dd8-#ddf; #de6-#def; #df2-#df4; #e01-#e3a; #e3f-#e5b; #e81-#e82; #e84; #e87-#e88; #e8a; #e8d; #e94-#e97; #e99-#e9f; #ea1-#ea3; #ea5; #ea7; #eaa-#eab; #ead-#eb9; #ebb-#ebd; #ec0-#ec4; #ec6; #ec8-#ecd; #ed0-#ed9; #edc-#edf; #f00-#f47; #f49-#f6c; #f71-#f97; #f99-#fbc; #fbe-#fcc; #fce-#fda; #1000-#10c5; #10c7; #10cd; #10d0-#1248; #124a-#124d; #1250-#1256; #1258; #125a-#125d; #1260-#1288; #128a-#128d; #1290-#12b0; #12b2-#12b5; #12b8-#12be; #12c0; #12c2-#12c5; #12c8-#12d6; #12d8-#1310; #1312-#1315; #1318-#135a; #135d-#137c; #1380-#1399; #13a0-#13f5; #13f8-#13fd; #1400-#169c; #16a0-#16f8; #1700-#170c; #170e-#1714; #1720-#1736; #1740-#1753; #1760-#176c; #176e-#1770; #1772-#1773; #1780-#17dd; #17e0-#17e9; #17f0-#17f9; #1800-#180e; #1810-#1819; #1820-#1877; #1880-#18aa; #18b0-#18f5; #1900-#191e; #1920-#192b; #1930-#193b; #1940; #1944-#196d; #1970-#1974; #1980-#19ab; #19b0-#19c9; #19d0-#19da; #19de-#1a1b; #1a1e-#1a5e; #1a60-#1a7c; #1a7f-#1a89; #1a90-#1a99; #1aa0-#1aad; #1ab0-#1abe; #1b00-#1b4b; #1b50-#1b7c; #1b80-#1bf3; #1bfc-#1c37; #1c3b-#1c49; #1c4d-#1c88; #1cc0-#1cc7; #1cd0-#1cf9; #1d00-#1df9; #1dfb-#1f15; #1f18-#1f1d; #1f20-#1f45; #1f48-#1f4d; #1f50-#1f57; #1f59; #1f5b; #1f5d; #1f5f-#1f7d; #1f80-#1fb4; #1fb6-#1fc4; #1fc6-#1fd3; #1fd6-#1fdb; #1fdd-#1fef; #1ff2-#1ff4; #1ff6-#1ffe; #2000-#2064; #2066-#2071; #2074-#208e; #2090-#209c; #20a0-#20bf; #20d0-#20f0; #2100-#218b; #2190-#2426; #2440-#244a; #2460-#2b73; #2b76-#2b95; #2b98-#2bb9; #2bbd-#2bc8; #2bca-#2bd2; #2bec-#2bef; #2c00-#2c2e; #2c30-#2c5e; #2c60-#2cf3; #2cf9-#2d25; #2d27; #2d2d; #2d30-#2d67; #2d6f-#2d70; #2d7f-#2d96; #2da0-#2da6; #2da8-#2dae; #2db0-#2db6; #2db8-#2dbe; #2dc0-#2dc6; #2dc8-#2dce; #2dd0-#2dd6; #2dd8-#2dde; #2de0-#2e49; #2e80-#2e99; #2e9b-#2ef3; #2f00-#2fd5; #2ff0-#2ffb; #3000-#303f; #3041-#3096; #3099-#30ff; #3105-#312e; #3131-#318e; #3190-#31ba; #31c0-#31e3; #31f0-#321e; #3220-#4db5; #4dc0-#9fea; #a000-#a48c; #a490-#a4c6; #a4d0-#a62b; #a640-#a6f7; #a700-#a7ae; #a7b0-#a7b7; #a7f7-#a82b; #a830-#a839; #a840-#a877; #a880-#a8c5; #a8ce-#a8d9; #a8e0-#a8fd; #a900-#a953; #a95f-#a97c; #a980-#a9cd; #a9cf-#a9d9; #a9de-#a9fe; #aa00-#aa36; #aa40-#aa4d; #aa50-#aa59; #aa5c-#aac2; #aadb-#aaf6; #ab01-#ab06; #ab09-#ab0e; #ab11-#ab16; #ab20-#ab26; #ab28-#ab2e; #ab30-#ab65; #ab70-#abed; #abf0-#abf9; #ac00-#d7a3; #d7b0-#d7c6; #d7cb-#d7fb; #e000-#fa6d; #fa70-#fad9; #fb00-#fb06; #fb13-#fb17; #fb1d-#fb36; #fb38-#fb3c; #fb3e; #fb40-#fb41; #fb43-#fb44; #fb46-#fbc1; #fbd3-#fd3f; #fd50-#fd8f; #fd92-#fdc7; #fdf0-#fdfd; #fe00-#fe19; #fe20-#fe52; #fe54-#fe66; #fe68-#fe6b; #fe70-#fe74; #fe76-#fefc; #feff; #ff01-#ffbe; #ffc2-#ffc7; #ffca-#ffcf; #ffd2-#ffd7; #ffda-#ffdc; #ffe0-#ffe6; #ffe8-#ffee; #fff9-#fffd; #10000-#1000b; #1000d-#10026; #10028-#1003a; #1003c-#1003d; #1003f-#1004d; #10050-#1005d; #10080-#100fa; #10100-#10102; #10107-#10133; #10137-#1018e; #10190-#1019b; #101a0; #101d0-#101fd; #10280-#1029c; #102a0-#102d0; #102e0-#102fb; #10300-#10323; #1032d-#1034a; #10350-#1037a; #10380-#1039d; #1039f-#103c3; #103c8-#103d5; #10400-#1049d; #104a0-#104a9; #104b0-#104d3; #104d8-#104fb; #10500-#10527; #10530-#10563; #1056f; #10600-#10736; #10740-#10755; #10760-#10767; #10800-#10805; #10808; #1080a-#10835; #10837-#10838; #1083c; #1083f-#10855; #10857-#1089e; #108a7-#108af; #108e0-#108f2; #108f4-#108f5; #108fb-#1091b; #1091f-#10939; #1093f; #10980-#109b7; #109bc-#109cf; #109d2-#10a03; #10a05-#10a06; #10a0c-#10a13; #10a15-#10a17; #10a19-#10a33; #10a38-#10a3a; #10a3f-#10a47; #10a50-#10a58; #10a60-#10a9f; #10ac0-#10ae6; #10aeb-#10af6; #10b00-#10b35; #10b39-#10b55; #10b58-#10b72; #10b78-#10b91; #10b99-#10b9c; #10ba9-#10baf; #10c00-#10c48; #10c80-#10cb2; #10cc0-#10cf2; #10cfa-#10cff; #10e60-#10e7e; #11000-#1104d; #11052-#1106f; #1107f-#110c1; #110d0-#110e8; #110f0-#110f9; #11100-#11134; #11136-#11143; #11150-#11176; #11180-#111cd; #111d0-#111df; #111e1-#111f4; #11200-#11211; #11213-#1123e; #11280-#11286; #11288; #1128a-#1128d; #1128f-#1129d; #1129f-#112a9; #112b0-#112ea; #112f0-#112f9; #11300-#11303; #11305-#1130c; #1130f-#11310; #11313-#11328; #1132a-#11330; #11332-#11333; #11335-#11339; #1133c-#11344; #11347-#11348; #1134b-#1134d; #11350; #11357; #1135d-#11363; #11366-#1136c; #11370-#11374; #11400-#11459; #1145b; #1145d; #11480-#114c7; #114d0-#114d9; #11580-#115b5; #115b8-#115dd; #11600-#11644; #11650-#11659; #11660-#1166c; #11680-#116b7; #116c0-#116c9; #11700-#11719; #1171d-#1172b; #11730-#1173f; #118a0-#118f2; #118ff; #11a00-#11a47; #11a50-#11a83; #11a86-#11a9c; #11a9e-#11aa2; #11ac0-#11af8; #11c00-#11c08; #11c0a-#11c36; #11c38-#11c45; #11c50-#11c6c; #11c70-#11c8f; #11c92-#11ca7; #11ca9-#11cb6; #11d00-#11d06; #11d08-#11d09; #11d0b-#11d36; #11d3a; #11d3c-#11d3d; #11d3f-#11d47; #11d50-#11d59; #12000-#12399; #12400-#1246e; #12470-#12474; #12480-#12543; #13000-#1342e; #14400-#14646; #16800-#16a38; #16a40-#16a5e; #16a60-#16a69; #16a6e-#16a6f; #16ad0-#16aed; #16af0-#16af5; #16b00-#16b45; #16b50-#16b59; #16b5b-#16b61; #16b63-#16b77; #16b7d-#16b8f; #16f00-#16f44; #16f50-#16f7e; #16f8f-#16f9f; #16fe0-#16fe1; #17000-#187ec; #18800-#18af2; #1b000-#1b11e; #1b170-#1b2fb; #1bc00-#1bc6a; #1bc70-#1bc7c; #1bc80-#1bc88; #1bc90-#1bc99; #1bc9c-#1bca3; #1d000-#1d0f5; #1d100-#1d126; #1d129-#1d1e8; #1d200-#1d245; #1d300-#1d356; #1d360-#1d371; #1d400-#1d454; #1d456-#1d49c; #1d49e-#1d49f; #1d4a2; #1d4a5-#1d4a6; #1d4a9-#1d4ac; #1d4ae-#1d4b9; #1d4bb; #1d4bd-#1d4c3; #1d4c5-#1d505; #1d507-#1d50a; #1d50d-#1d514; #1d516-#1d51c; #1d51e-#1d539; #1d53b-#1d53e; #1d540-#1d544; #1d546; #1d54a-#1d550; #1d552-#1d6a5; #1d6a8-#1d7cb; #1d7ce-#1da8b; #1da9b-#1da9f; #1daa1-#1daaf; #1e000-#1e006; #1e008-#1e018; #1e01b-#1e021; #1e023-#1e024; #1e026-#1e02a; #1e800-#1e8c4; #1e8c7-#1e8d6; #1e900-#1e94a; #1e950-#1e959; #1e95e-#1e95f; #1ee00-#1ee03; #1ee05-#1ee1f; #1ee21-#1ee22; #1ee24; #1ee27; #1ee29-#1ee32; #1ee34-#1ee37; #1ee39; #1ee3b; #1ee42; #1ee47; #1ee49; #1ee4b; #1ee4d-#1ee4f; #1ee51-#1ee52; #1ee54; #1ee57; #1ee59; #1ee5b; #1ee5d; #1ee5f; #1ee61-#1ee62; #1ee64; #1ee67-#1ee6a; #1ee6c-#1ee72; #1ee74-#1ee77; #1ee79-#1ee7c; #1ee7e; #1ee80-#1ee89; #1ee8b-#1ee9b; #1eea1-#1eea3; #1eea5-#1eea9; #1eeab-#1eebb; #1eef0-#1eef1; #1f000-#1f02b; #1f030-#1f093; #1f0a0-#1f0ae; #1f0b1-#1f0bf; #1f0c1-#1f0cf; #1f0d1-#1f0f5; #1f100-#1f10c; #1f110-#1f12e; #1f130-#1f16b; #1f170-#1f1ac; #1f1e6-#1f202; #1f210-#1f23b; #1f240-#1f248; #1f250-#1f251; #1f260-#1f265; #1f300-#1f6d4; #1f6e0-#1f6ec; #1f6f0-#1f6f8; #1f700-#1f773; #1f780-#1f7d4; #1f800-#1f80b; #1f810-#1f847; #1f850-#1f859; #1f860-#1f887; #1f890-#1f8ad; #1f900-#1f90b; #1f910-#1f93e; #1f940-#1f94c; #1f950-#1f96b; #1f980-#1f997; #1f9c0; #1f9d0-#1f9e6; #20000-#2a6d6; #2a700-#2b734; #2b740-#2b81d; #2b820-#2cea1; #2ceb0-#2ebe0; #2f800-#2fa1d; #e0001; #e0020-#e007f; #e0100-#e01ef; #f0000-#ffffd; #100000-#10fffd]";
    assertEquals(stringOfAlphabet, RangeSet.ALPHABET.toString());
  }

  @Test
  public void testIntersect() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().addAll(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected = RangeSet.builder().addAll(transformIntegerToBitRanges(i & j)).build();
        RangeSet rhs = RangeSet.builder().addAll(transformIntegerToBitRanges(j)).build();
        RangeSet intersection = lhs.intersection(rhs);
        assertEquals(expected , intersection, lhs + " intersect " + rhs + " returns " + intersection);
      }
    }
  }

  @Test
  public void testUnion() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().addAll(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected = RangeSet.builder().addAll(transformIntegerToBitRanges(i | j)).build();
        RangeSet rhs = RangeSet.builder().addAll(transformIntegerToBitRanges(j)).build();
        RangeSet union = lhs.union(rhs);
        assertEquals(expected , union, lhs + " union " + rhs + " returns " + union);
      }
    }
  }

  @Test
  public void testMinus() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().addAll(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected1 = RangeSet.builder().addAll(transformIntegerToBitRanges(i & ~j)).build();
        RangeSet rhs = RangeSet.builder().addAll(transformIntegerToBitRanges(j)).build();
        RangeSet minus = lhs.minus(rhs);
        assertEquals(expected1 , minus, lhs + " minus " + rhs + " returns " + minus);
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void testContainsCodepoint(String name, RangeSet rangeSet) {
    for (Range range : rangeSet)
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        assertTrue(rangeSet.containsCodepoint(codepoint));
    RangeSet complement = rangeSet.complement();
    for (Range range : rangeSet)
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        assertFalse(complement.containsCodepoint(codepoint));
  }

  public static Stream<Arguments> testContainsCodepoint() {
    return RangeSet.unicodeClasses.entrySet().stream()
      .map(e -> Arguments.of(e.getKey(), e.getValue()));
  }

}
