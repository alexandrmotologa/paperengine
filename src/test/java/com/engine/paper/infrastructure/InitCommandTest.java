package com.engine.paper.infrastructure;

import com.engine.paper.domain.model.Document;
import com.engine.paper.infrastructure.scaffold.ScaffoldTemplates;
import com.engine.paper.template.TemplateCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class InitCommandTest {

    @Test
    @DisplayName("Should scaffold all 4 preset templates and compile them successfully")
    void testScaffoldPresets(@TempDir Path tempDir) throws IOException {
        String[] types = {"invoice", "shipping-label", "certificate", "financial-report"};
        TemplateCompiler compiler = new TemplateCompiler();

        for (String type : types) {
            File subDir = tempDir.resolve(type).toFile();
            ScaffoldTemplates.scaffoldToDirectory(type, subDir);

            ScaffoldTemplates.ScaffoldResult res = ScaffoldTemplates.get(type);
            File tFile = new File(subDir, res.templateFileName());
            File dFile = new File(subDir, res.dataFileName());

            assertThat(tFile).exists();
            assertThat(dFile).exists();

            String template = Files.readString(tFile.toPath());
            String data = Files.readString(dFile.toPath());

            Document doc = compiler.compile(template, data);
            assertThat(doc).isNotNull();
            assertThat(doc.getBody().getChildren()).isNotEmpty();
        }
    }
}
