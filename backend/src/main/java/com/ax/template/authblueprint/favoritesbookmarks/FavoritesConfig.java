package com.ax.template.authblueprint.favoritesbookmarks;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FavoritesProperties.class)
public class FavoritesConfig {
}
