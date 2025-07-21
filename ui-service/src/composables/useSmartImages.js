/**
 * 운영용 스마트 이미지 시스템
 * - 관리자 업로드 이미지: API Gateway를 통해 서버에서
 * - 기본 이미지들: 프론트엔드 static 파일에서
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'  // 프론트엔드 static 파일
    const API_GATEWAY_URL = import.meta.env.VITE_API_BASE_URL || 'http://13.209.253.241:8080'  // 환경변수 사용

    const getProductImage = (product) => {
        console.log('🔍 getProductImage 호출됨:', product)

        // 🔥 관리자가 업로드한 이미지 - API Gateway를 통해 서버에서 가져오기
        if (product.mainImage && product.mainImage.startsWith('/upload/')) {
            const fileName = product.mainImage.split('/').pop()
            const finalUrl = `${API_GATEWAY_URL}/images/${fileName}`
            console.log('✅ 서버 업로드 이미지 (mainImage):', finalUrl)
            return finalUrl
        }

        // 🔥 image 필드도 업로드된 파일인 경우
        if (product.image && product.image.startsWith('/upload/')) {
            const fileName = product.image.split('/').pop()
            const finalUrl = `${API_GATEWAY_URL}/images/${fileName}`
            console.log('✅ 서버 업로드 이미지 (image):', finalUrl)
            return finalUrl
        }

        // 🔥 파일명만 있는 경우 (관리자 업로드)
        if (product.mainImage && !product.mainImage.startsWith('/') && !product.mainImage.startsWith('http')) {
            // UUID 패턴인지 확인 (관리자가 업로드한 파일)
            if (product.mainImage.includes('-') && product.mainImage.length > 30) {
                const finalUrl = `${API_GATEWAY_URL}/images/${product.mainImage}`
                console.log('✅ 서버 업로드 파일명 (mainImage):', finalUrl)
                return finalUrl
            } else {
                // 일반 파일명은 프론트엔드 static 파일
                const finalUrl = `${BASE_IMAGE_PATH}${product.mainImage}`
                console.log('✅ 프론트엔드 static 파일 (mainImage):', finalUrl)
                return finalUrl
            }
        }

        // 🔥 image 필드가 파일명만 있는 경우
        if (product.image && !product.image.startsWith('/') && !product.image.startsWith('http')) {
            // UUID 패턴인지 확인 (관리자가 업로드한 파일)
            if (product.image.includes('-') && product.image.length > 30) {
                const finalUrl = `${API_GATEWAY_URL}/images/${product.image}`
                console.log('✅ 서버 업로드 파일명 (image):', finalUrl)
                return finalUrl
            } else {
                // 일반 파일명은 프론트엔드 static 파일
                const finalUrl = `${BASE_IMAGE_PATH}${product.image}`
                console.log('✅ 프론트엔드 static 파일 (image):', finalUrl)
                return finalUrl
            }
        }

        // 이미 완전한 URL인 경우 (http로 시작하는 외부 이미지)
        if (product.image && product.image.startsWith('http')) {
            console.log('✅ 외부 이미지:', product.image)
            return product.image
        }

        // 이미 완전한 경로인 경우 (/images로 시작) - 프론트엔드 static 파일
        if (product.image && product.image.startsWith('/images')) {
            console.log('✅ 프론트엔드 static 이미지:', product.image)
            return product.image
        }

        // 🔥 기본 이미지 - 프론트엔드 static 파일
        const defaultImage = `${BASE_IMAGE_PATH}default-product.jpg`
        console.log('⚠️ 기본 이미지 사용:', defaultImage)
        return defaultImage
    }

    const handleImageError = (event) => {
        console.log('❌ 이미지 로드 실패:', event.target.src)

        // 첫 번째 실패: 프론트엔드 기본 이미지로 교체
        if (!event.target.src.includes('default-product.jpg')) {
            console.log('🔄 프론트엔드 기본 이미지로 교체')
            event.target.src = `${BASE_IMAGE_PATH}default-product.jpg`
        } else {
            // 기본 이미지도 실패하면 대체 이미지나 숨김 처리
            console.log('🔄 최종 대체 이미지 사용')
            // 최종 대체: 간단한 SVG 데이터 URL 또는 숨김
            event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='
        }
    }

    const handleImageLoad = (event) => {
        console.log('✅ 이미지 로드 성공:', event.target.src)
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}