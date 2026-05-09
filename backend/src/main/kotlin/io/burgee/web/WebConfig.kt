package io.burgee.web

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(false)
            .addResolver(SpaResourceResolver())
    }
}

private class SpaResourceResolver : PathResourceResolver() {

    private val index: Resource = ClassPathResource("static/index.html")

    override fun getResource(resourcePath: String, location: Resource): Resource? {
        val resolved = super.getResource(resourcePath, location)
        if (resolved != null) return resolved
        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) return null
        return if (index.exists()) index else null
    }
}
