package com.GSU26SE22_SU26SE002.RealMateAI.enums;
/**
 * Tiêu chí sắp xếp cho POST /listings/search.
 * NEWEST mặc định nếu không truyền hoặc truyền null.
 */

public enum ListingSortEnum {
    NEWEST,
    OLDEST,
    PRICE_ASC,
    PRICE_DESC,
    AREA_ASC,
    AREA_DESC,
    MOST_VIEWED
}
