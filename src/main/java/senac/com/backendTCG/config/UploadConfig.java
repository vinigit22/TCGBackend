package senac.com.backendTCG.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import senac.com.backendTCG.service.ArmazenamentoImagemService;

// Serve as imagens enviadas: GET /uploads/** le os arquivos da pasta app.upload.diretorio
@Configuration
@RequiredArgsConstructor
public class UploadConfig implements WebMvcConfigurer {

    private final ArmazenamentoImagemService armazenamentoImagemService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String local = armazenamentoImagemService.getDiretorio().toUri().toString();
        registry.addResourceHandler(ArmazenamentoImagemService.PREFIXO_PUBLICO + "**")
                .addResourceLocations(local.endsWith("/") ? local : local + "/");
    }
}
