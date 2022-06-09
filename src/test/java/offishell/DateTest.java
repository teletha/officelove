/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell;

import org.junit.jupiter.api.Test;

public class DateTest {

    @Test
    void compare() {
        Date one = Date.of(10, 1);
        Date other = Date.of(1, 20);
        assert one.compareTo(other) > 0;
        assert other.compareTo(one) < 0;
        assert one.compareTo(one) == 0;
    }
}
