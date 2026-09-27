package BloodBridge.config;

import BloodBridge.common.BloodGroup;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToBloodGroupConverter());
    }

    public static class StringToBloodGroupConverter implements Converter<String, BloodGroup> {
        @Override
        public BloodGroup convert(String source) {
            return BloodGroup.from(source);
        }
    }
}
