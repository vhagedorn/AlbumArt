package me.vadim.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a magic constant parameter.
 * @author vadim
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface MagicConst {}