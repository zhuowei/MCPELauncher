package net.zhuoweizhang.mcpelauncher.api.modpe;
import java.lang.annotation.*;
@Retention(value=RetentionPolicy.RUNTIME)
public @interface CallbackName {
	String name();
	String[] args() default {};
	boolean prevent() default false;
}
